package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Script
import com.example.data.model.VideoRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Script::class, VideoRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun videoRecordDao(): VideoRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dualprompter_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialScripts(database.scriptDao())
                    }
                }
            }

            suspend fun populateInitialScripts(scriptDao: ScriptDao) {
                scriptDao.insertScript(
                    Script(
                        title = "🚀 YouTube Tech Review Hook & Intro",
                        content = """Hey everyone, welcome back to the channel! Today, we are diving deep into the most requested camera and teleprompter setup of the year.

If you have ever struggled to keep direct eye contact with your audience while reading a detailed script, this is for you.

Notice how natural this feels? Because the script is right near the camera lens, you never look like you are reading.

Let's test both vertical 9:16 for YouTube Shorts and horizontal 16:9 for the main channel video at the exact same time.

Smash that like button, subscribe, and let's jump right into the full walkthrough!""",
                        fontSizeSp = 24f,
                        scrollSpeedPxPerSec = 42f,
                        textColorHex = "#38BDF8",
                        bgOpacityPercent = 80,
                        textAlignment = "CENTER"
                    )
                )

                scriptDao.insertScript(
                    Script(
                        title = "📱 60-Second TikTok / Reels Quick Pitch",
                        content = """Stop recording horizontal and vertical videos separately!

Here is how top creators save 10 hours every week:
Dual Recording Mode.

One camera take captures your widescreen 16:9 YouTube video AND your 9:16 vertical TikTok simultaneously.

Look right at the lens. Read your script smoothly without memorizing a single word.

Save your take once. Post to all platforms in seconds.

Follow for more creator studio secrets!""",
                        fontSizeSp = 26f,
                        scrollSpeedPxPerSec = 50f,
                        textColorHex = "#FACC15",
                        bgOpacityPercent = 75,
                        textAlignment = "CENTER"
                    )
                )

                scriptDao.insertScript(
                    Script(
                        title = "🎓 Online Course / Educational Welcome",
                        content = """Hello students, and welcome to Module One.

In this lesson, we will explore key concepts that will serve as the foundation for the entire semester.

Please make sure you have downloaded the study syllabus and have your notebook ready.

By the end of this video, you will understand:
First: How the dual-frame recording pipeline optimizes creator workflows.
Second: Why eye contact builds higher audience retention and engagement.
Third: How to structure your scripts for concise delivery.

Let's begin with our first core principle.""",
                        fontSizeSp = 22f,
                        scrollSpeedPxPerSec = 38f,
                        textColorHex = "#FFFFFF",
                        bgOpacityPercent = 80,
                        textAlignment = "LEFT"
                    )
                )
            }
        }
    }
}
