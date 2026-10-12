package com.pp.smarthealth;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.assertj.core.api.Assertions.*;

class HealthTimelineMigrationTest {
    @Test void additiveMigrationPreservesOldRowsAndAuditAfterDoctorDeletion() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:timeline-migration;MODE=MySQL", "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE patient(id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE doctors(id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE ai_consultations(id BIGINT PRIMARY KEY, patient_id BIGINT, ai_response TEXT)");
            statement.execute("INSERT INTO patient VALUES(1)");
            statement.execute("INSERT INTO doctors VALUES(2)");
            statement.execute("INSERT INTO ai_consultations VALUES(3,1,'original symptom analysis')");
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("docs/db/V2__patient_health_timeline.sql"));
            try (var rows = statement.executeQuery("SELECT ai_response FROM ai_consultations WHERE id=3")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("original symptom analysis");
            }
            statement.execute("INSERT INTO health_change_analyses(id,patient_id,response_json,model_name,created_at) VALUES(4,1,'{}','test',CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO doctor_reviews(id,analysis_id,doctor_id,reviewer_doctor_id,status,reviewed_at) VALUES(5,4,2,2,'REVIEWED',CURRENT_TIMESTAMP)");
            statement.execute("DELETE FROM doctors WHERE id=2");
            try (var rows = statement.executeQuery("SELECT doctor_id,reviewer_doctor_id FROM doctor_reviews WHERE id=5")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject(1)).isNull();
                assertThat(rows.getLong(2)).isEqualTo(2);
            }
            statement.execute("DELETE FROM patient WHERE id=1");
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM doctor_reviews")) {
                rows.next(); assertThat(rows.getLong(1)).isZero();
            }
        }
    }
}
