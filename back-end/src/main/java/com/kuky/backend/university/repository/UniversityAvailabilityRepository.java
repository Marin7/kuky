package com.kuky.backend.university.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class UniversityAvailabilityRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public UniversityAvailabilityRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
    public boolean homeworkAvailable(UUID id,String level){return exists("university_homework_availability","assignment_id",id,level);}
    public boolean presentationAvailable(UUID id,String level){return exists("university_presentation_availability","presentation_id",id,level);}
    private boolean exists(String table,String column,UUID id,String level){Integer count=jdbc.queryForObject("SELECT COUNT(1) FROM "+table+" WHERE "+column+"=:id AND level=:level",Map.of("id",id,"level",level),Integer.class);return count!=null&&count>0;}
    public List<UUID> ids(String type,String level){String table=type.equals("homeworks")?"university_homework_availability":"university_presentation_availability";String column=type.equals("homeworks")?"assignment_id":"presentation_id";return jdbc.query("SELECT "+column+" FROM "+table+" WHERE level=:level",Map.of("level",level),(r,n)->r.getObject(1,UUID.class));}
    public void replace(String type,String level,List<UUID> ids){String table=type.equals("homeworks")?"university_homework_availability":"university_presentation_availability";String column=type.equals("homeworks")?"assignment_id":"presentation_id";jdbc.update("DELETE FROM "+table+" WHERE level=:level",Map.of("level",level));for(UUID id:ids)jdbc.update("INSERT INTO "+table+"("+column+",level) VALUES(:id,:level) ON CONFLICT DO NOTHING",Map.of("id",id,"level",level));}
}
