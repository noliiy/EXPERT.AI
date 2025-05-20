package storage;

import config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.sql.SQLException;

/**
 * Data Access Object for the 'student' table.
 * Provides methods to insert/update (upsert) a student,
 * retrieve profile data by Discord ID, delete a profile,
 * and update the student's resume (CV) text.
 */
public class StudentDAO {

    /**
     * Inserts a new student record or updates an existing one based on the Discord ID.
     * Only non-null fields in the upsert call will be updated; others are preserved.
     *
     * @param fullName        student's full name
     * @param email           student's email
     * @param skills          list of skills as a string
     * @param careerInterest  preferred career field
     * @param discordId       unique Discord user ID (primary key)
     * @throws Exception if the database operation fails
     */
    public static void upsertStudent(
            String fullName,
            String email,
            String skills,
            String careerInterest,
            String discordId
    ) throws Exception {
        String sql = """
            INSERT INTO student
                (name, email, skills, career_interest, discord_id)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (discord_id) DO UPDATE
              SET name            = COALESCE(EXCLUDED.name,            student.name),
                  email           = COALESCE(EXCLUDED.email,           student.email),
                  skills          = COALESCE(EXCLUDED.skills,          student.skills),
                  career_interest = COALESCE(EXCLUDED.career_interest, student.career_interest)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, fullName, Types.VARCHAR);
            stmt.setObject(2, email, Types.VARCHAR);
            stmt.setObject(3, skills, Types.VARCHAR);
            stmt.setObject(4, careerInterest, Types.VARCHAR);
            stmt.setString(5, discordId);

            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves a student's profile information from the database using their Discord ID.
     *
     * @param discordId the unique Discord user ID
     * @return a map with keys like "Name", "Email", "Skills", etc., or null if not found
     * @throws Exception if the database query fails
     */
    public static Map<String, String> getStudentProfile(String discordId) throws Exception {
        String sql = "SELECT name, email, skills, career_interest FROM student WHERE discord_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> profile = new HashMap<>();
                    profile.put("Name", rs.getString("name"));
                    profile.put("Email", rs.getString("email"));
                    profile.put("Skills", rs.getString("skills"));
                    profile.put("Career Interest", rs.getString("career_interest"));
                    return profile;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Deletes a student's profile from the database using their Discord ID.
     *
     * @param discordId the user's Discord ID
     * @return true if a row was deleted, false if no match was found
     */
    public static boolean deleteProfileByDiscordId(String discordId) {
        String sql = "DELETE FROM student WHERE discord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, discordId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the `cv_text` column for a student given their Discord ID.
     *
     * @param discordId the user's Discord ID
     * @param cvText    the extracted plain text content of the resume
     * @throws Exception if the update fails
     */
    public static void updateCvTextByDiscordId(String discordId, String cvText) throws Exception {
        if (cvText == null || cvText.isBlank()) {
            System.out.println("⚠️ Skipping CV text update: input is null or blank.");
            return;
        }

        String sql = """
        INSERT INTO student (discord_id, cv_text)
        VALUES (?, ?)
        ON CONFLICT (discord_id) DO UPDATE
        SET cv_text = EXCLUDED.cv_text
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, discordId);
            pstmt.setString(2, cvText);

            int rows = pstmt.executeUpdate();
            System.out.println("✅ CV upserted for " + discordId + " (rows affected: " + rows + ")");
        }
    }

}
