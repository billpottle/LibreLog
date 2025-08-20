package com.example.librelog;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventTypeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(EventType eventType);

    @Update
    void update(EventType eventType);

    @Delete
    void delete(EventType eventType);

    @Query("SELECT * FROM event_types ORDER BY event_name ASC")
    List<EventType> getAllEventTypes();

    @Query("SELECT * FROM event_types WHERE event_name = :name LIMIT 1")
    EventType findByName(String name);

    @Query("SELECT * FROM event_types WHERE event_type_id = :id LIMIT 1")
    EventType findById(int id);
}