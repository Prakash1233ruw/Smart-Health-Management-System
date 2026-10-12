package com.pp.smarthealth.service;

import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.pp.smarthealth.exception.BadRequestException;

@Component
public class ReportDocumentReader {
    public record Document(List<String> pageText, List<String> images, String mode) {}
    public Document read(MultipartFile file) {
        if(file==null || file.isEmpty() || file.getSize()>8*1024*1024)
            throw new BadRequestException("Upload a nonempty PDF, PNG, or JPEG up to 8 MB.");
        try {
            byte[] bytes=file.getBytes();
            if(bytes.length>=5 && new String(bytes,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
                try(var pdf=Loader.loadPDF(bytes)) {
                    if(pdf.isEncrypted() || pdf.getNumberOfPages()<1 || pdf.getNumberOfPages()>5)
                        throw new BadRequestException("Use an unencrypted PDF with 1–5 pages.");
                    List<String> texts=new ArrayList<>();
                    var stripper=new PDFTextStripper();
                    for(int i=1;i<=pdf.getNumberOfPages();i++) { stripper.setStartPage(i);stripper.setEndPage(i);texts.add(stripper.getText(pdf)); }
                    if(texts.stream().mapToInt(String::length).sum()>50000) throw new BadRequestException("Report text is too large. Upload fewer pages.");
                    // Render every page if any page lacks text, so mixed/scanned PDFs are not silently skipped.
                    if(texts.stream().allMatch(t->t.trim().length()>=30)) return new Document(texts,List.of(),"PDF_TEXT");
                    List<String> images=new ArrayList<>(); var renderer=new PDFRenderer(pdf);
                    for(int i=0;i<pdf.getNumberOfPages();i++) {
                        var box=pdf.getPage(i).getCropBox();
                        if(box.getWidth()<=0 || box.getHeight()<=0 || box.getWidth()*box.getHeight()*Math.pow(100.0/72,2)>4000000)
                            throw new BadRequestException("A PDF page is too large to process.");
                        var image=renderer.renderImageWithDPI(i,100);
                        try(var out=new ByteArrayOutputStream()) { ImageIO.write(image,"png",out); images.add(dataUrl("image/png",out.toByteArray())); }
                        if(images.stream().mapToLong(String::length).sum()>16*1024*1024) throw new BadRequestException("Rendered report is too large. Upload fewer pages or a smaller image.");
                    }
                    return new Document(texts,images,"PDF_VISION");
                }
            }
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                if(!readers.hasNext()) throw new BadRequestException("The file is not a readable PDF, PNG, or JPEG.");
                var reader=readers.next();
                try {
                    reader.setInput(stream);
                    String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!List.of("png","jpeg","jpg").contains(format) || (long)reader.getWidth(0)*reader.getHeight(0)>12000000)
                        throw new BadRequestException("Use a PNG or JPEG with no more than 12 million pixels.");
                    // Decode and re-encode before sending, dropping unrelated image metadata.
                    var image=reader.read(0);
                    try(var out=new ByteArrayOutputStream()) { ImageIO.write(image,"png",out); return new Document(List.of(""),List.of(dataUrl("image/png",out.toByteArray())),"IMAGE_VISION"); }
                } finally { reader.dispose(); }
            }
        } catch(BadRequestException ex) { throw ex; }
        catch(Exception ex) { throw new BadRequestException("The report could not be read. Use an unencrypted PDF, PNG, or JPEG."); }
    }
    private String dataUrl(String mime,byte[] data) {
        if(data.length>10*1024*1024) throw new BadRequestException("Image is too large after processing. Upload a smaller image.");
        return "data:"+mime+";base64,"+Base64.getEncoder().encodeToString(data);
    }
}
