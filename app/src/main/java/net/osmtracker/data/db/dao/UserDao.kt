package net.osmtracker.data.db.dao

//import androidx.room.Dao
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
import net.osmtracker.data.db.model.UserEntity

//@Dao
interface UserDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

//    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: Int): UserEntity?
}