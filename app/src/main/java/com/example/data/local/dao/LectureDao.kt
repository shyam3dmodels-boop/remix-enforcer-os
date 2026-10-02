package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LectureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {
    @Query("SELECT * FROM lecture_recordings ORDER BY timestamp DESC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lecture_recordings ORDER BY timestamp DESC")
    suspend fun getAllLecturesDirect(): List<LectureEntity>

    @Query("SELECT * FROM lecture_recordings WHERE id = :id LIMIT 1")
    suspend fun getLectureById(id: Long): LectureEntity?

    @Query("SELECT * FROM lecture_recordings WHERE filePath = :filePath LIMIT 1")
    suspend fun getLectureByFilePath(filePath: String): LectureEntity?

    @Query("SELECT * FROM lecture_recordings WHERE subject = :subject ORDER BY timestamp DESC")
    fun getLecturesBySubject(subject: String): Flow<List<LectureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    @Query("DELETE FROM lecture_recordings WHERE id = :id")
    suspend fun deleteLectureById(id: Long)

    @Query("SELECT COUNT(*) FROM lecture_recordings")
    suspend fun getCount(): Int
}
