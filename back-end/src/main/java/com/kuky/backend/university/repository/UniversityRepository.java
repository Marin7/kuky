package com.kuky.backend.university.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class UniversityRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public UniversityRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Session(UUID id, String level, int dayOfWeek, LocalTime startTime, LocalTime endTime, String title) {}
    public record ExceptionRow(UUID id, String level, LocalDate exceptionDate, String kind, UUID sessionId,
                               LocalTime startTime, LocalTime endTime, String title) {}
    public record Exam(UUID id, String title, Instant examAt, String description, boolean published) {}
    public record News(UUID id, String title, String body, boolean published, Instant publishedAt) {}

    public List<Session> sessions(String level) {
        String sql = "SELECT * FROM university_schedule_sessions " + (level == null ? "" : "WHERE level=:level ") + "ORDER BY day_of_week,start_time";
        return jdbc.query(sql, level == null ? Map.of() : Map.of("level", level), (r,n) -> new Session(r.getObject("id", UUID.class), r.getString("level"), r.getInt("day_of_week"), r.getObject("start_time", LocalTime.class), r.getObject("end_time", LocalTime.class), r.getString("title")));
    }
    public List<ExceptionRow> exceptions(LocalDate from, LocalDate to, String level) {
        String sql = "SELECT * FROM university_schedule_exceptions WHERE exception_date BETWEEN :from AND :to" + (level == null ? "" : " AND level=:level") + " ORDER BY exception_date,start_time";
        Map<String,Object> p = new java.util.HashMap<>(); p.put("from", from); p.put("to", to); if(level != null)p.put("level",level);
        return jdbc.query(sql,p,(r,n)->new ExceptionRow(r.getObject("id",UUID.class),r.getString("level"),r.getObject("exception_date",LocalDate.class),r.getString("kind"),r.getObject("session_id",UUID.class),r.getObject("start_time",LocalTime.class),r.getObject("end_time",LocalTime.class),r.getString("title")));
    }
    public UUID saveSession(UUID id,String level,int day,LocalTime start,LocalTime end,String title) {
        if(id==null){id=UUID.randomUUID(); jdbc.update("INSERT INTO university_schedule_sessions(id,level,day_of_week,start_time,end_time,title) VALUES(:id,:level,:day,:start,:end,:title)",Map.of("id",id,"level",level,"day",day,"start",start,"end",end,"title",title==null?"":title));}
        else jdbc.update("UPDATE university_schedule_sessions SET level=:level,day_of_week=:day,start_time=:start,end_time=:end,title=:title,updated_at=NOW() WHERE id=:id",Map.of("id",id,"level",level,"day",day,"start",start,"end",end,"title",title==null?"":title));
        return id;
    }
    public UUID saveException(String level,LocalDate date,String kind,UUID sessionId,LocalTime start,LocalTime end,String title) {
        UUID id=UUID.randomUUID(); Map<String,Object> p=new java.util.HashMap<>();p.put("id",id);p.put("level",level);p.put("date",date);p.put("kind",kind);p.put("sessionId",sessionId);p.put("start",start);p.put("end",end);p.put("title",title);
        jdbc.update("INSERT INTO university_schedule_exceptions(id,level,exception_date,kind,session_id,start_time,end_time,title) VALUES(:id,:level,:date,:kind,:sessionId,:start,:end,:title)",p); return id;
    }
    public void delete(String table, UUID id){ jdbc.update("DELETE FROM "+table+" WHERE id=:id",Map.of("id",id)); }
    public List<Exam> exams(boolean all){return jdbc.query("SELECT * FROM university_exam_dates "+(all?"":"WHERE published=true ")+"ORDER BY exam_at",Map.of(),(r,n)->new Exam(r.getObject("id",UUID.class),r.getString("title"),r.getTimestamp("exam_at").toInstant(),r.getString("description"),r.getBoolean("published"))); }
    public UUID saveExam(UUID id,String title,Instant at,String desc,boolean published){if(id==null){id=UUID.randomUUID();jdbc.update("INSERT INTO university_exam_dates(id,title,exam_at,description,published) VALUES(:id,:title,:at,:description,:published)",Map.of("id",id,"title",title,"at",at,"description",desc==null?"":desc,"published",published));}else jdbc.update("UPDATE university_exam_dates SET title=:title,exam_at=:at,description=:description,published=:published,updated_at=NOW() WHERE id=:id",Map.of("id",id,"title",title,"at",at,"description",desc==null?"":desc,"published",published));return id;}
    public List<News> news(boolean all){return jdbc.query("SELECT * FROM university_news_items "+(all?"":"WHERE published=true ")+"ORDER BY published_at DESC NULLS LAST,created_at DESC",Map.of(),(r,n)->new News(r.getObject("id",UUID.class),r.getString("title"),r.getString("body"),r.getBoolean("published"),r.getTimestamp("published_at")==null?null:r.getTimestamp("published_at").toInstant()));}
    public UUID saveNews(UUID id,String title,String body,boolean published,Instant publishedAt){if(id==null){id=UUID.randomUUID();} Map<String,Object>p=new java.util.HashMap<>();p.put("id",id);p.put("title",title);p.put("body",body);p.put("published",published);p.put("publishedAt",publishedAt); if(jdbc.update("UPDATE university_news_items SET title=:title,body=:body,published=:published,published_at=:publishedAt,updated_at=NOW() WHERE id=:id",p)==0)jdbc.update("INSERT INTO university_news_items(id,title,body,published,published_at) VALUES(:id,:title,:body,:published,:publishedAt)",p);return id;}
}
