package com.pp.smarthealth.service;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class ReportDocumentReaderTest {
    private final ReportDocumentReader reader=new ReportDocumentReader();
    @Test void pngAndScannedPdfBecomeVisionInputsWithoutTrustingContentType() throws Exception {
        try(var out=new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(80,80,BufferedImage.TYPE_INT_RGB),"png",out);
            var doc=reader.read(new MockMultipartFile("file","image.txt","text/plain",out.toByteArray()));
            assertThat(doc.mode()).isEqualTo("IMAGE_VISION");assertThat(doc.images().get(0)).startsWith("data:image/png;base64,");
        }
        try(var pdf=new PDDocument();var out=new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());pdf.save(out);
            var doc=reader.read(new MockMultipartFile("file","scanned.pdf","application/pdf",out.toByteArray()));
            assertThat(doc.mode()).isEqualTo("PDF_VISION");assertThat(doc.images()).hasSize(1);
        }
    }
    @Test void fileSizeLimitIsCheckedBeforeParsing() {
        assertThatThrownBy(()->reader.read(new MockMultipartFile("file","oversized.png","image/png",new byte[8*1024*1024+1])))
            .isInstanceOf(com.pp.smarthealth.exception.BadRequestException.class).hasMessageContaining("8 MB");
    }
}
