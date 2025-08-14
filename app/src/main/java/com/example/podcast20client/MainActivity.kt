package com.example.podcast20client

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

data class Episode(val title: String, val audioUrl: String, val description: String, val chapters: List<String>)

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private var episodes = mutableListOf<Episode>()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.episode_list)

        // Sample Podcast 2.0 feed URL (replace with any valid podcast feed)
        val feedUrl = "https://feeds.podnews.net/podcast.xml"

        CoroutineScope(Dispatchers.IO).launch {
            episodes = parsePodcastFeed(feedUrl)
            withContext(Dispatchers.Main) {
                val titles = episodes.map { it.title }
                listView.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, titles)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val episode = episodes[position]
            playAudio(episode.audioUrl)
            Toast.makeText(this, "Playing: ${episode.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    private suspend fun parsePodcastFeed(feedUrl: String): MutableList<Episode> = withContext(Dispatchers.IO) {
        val episodeList = mutableListOf<Episode>()

        try {
            val url = URL(feedUrl)
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(url.openStream(), null)

            var eventType = parser.eventType
            var insideItem = false
            var title = ""
            var audioUrl = ""
            var description = ""
            val chapters = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> insideItem = true
                            "title" -> if (insideItem) title = parser.nextText()
                            "enclosure" -> if (insideItem) audioUrl = parser.getAttributeValue(null, "url") ?: ""
                            "description" -> if (insideItem) description = parser.nextText()
                            "podcast:chapters" -> {
                                // For simplicity, not parsing chapters fully here
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "item") {
                        episodeList.add(Episode(title, audioUrl, description, chapters.toList()))
                        insideItem = false
                        title = ""
                        audioUrl = ""
                        description = ""
                        chapters.clear()
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        episodeList
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}

