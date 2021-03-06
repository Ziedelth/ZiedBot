package fr.ziedelth.ziedbot.platforms

import fr.ziedelth.ziedbot.utils.Const
import fr.ziedelth.ziedbot.utils.ISO8601
import fr.ziedelth.ziedbot.utils.animes.*
import org.jsoup.Jsoup
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.awt.Color
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class Crunchyroll : Platform {
    override fun getName(): String = "Crunchyroll"
    override fun getURL(): String = "https://www.crunchyroll.com/"
    override fun getImage(): String =
        "https://archive.org/download/crunchyroll.-1.1.0/Crunchyroll.1.1.0/ico_android_settings.png"

    override fun getColor(): Color = Color(255, 108, 0)
    override fun getAllowedCountries(): Array<Country> = arrayOf(Country.FRANCE, Country.UNITED_STATES)

    override fun getLastNews(): Array<News> {
        val calendar = Calendar.getInstance()
        val l: MutableList<News> = mutableListOf()

        Country.values().filter { this.getAllowedCountries().contains(it) }.forEach { country ->
            val url: URLConnection
            val list: NodeList

            try {
                url = URL("https://www.crunchyroll.com/newsrss?lang=${country.lang}").openConnection()
                val dbf = DocumentBuilderFactory.newInstance()
                dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                val db = dbf.newDocumentBuilder()
                val doc = db.parse(url.getInputStream())
                doc.documentElement.normalize()
                list = doc.getElementsByTagName("item")
            } catch (exception: Exception) {
                return l.toTypedArray()
            }

            for (i in 0 until list.length) {
                val node = list.item(i)

                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element

                    val date = element.getElementsByTagName("pubDate").item(0).textContent
                    val releaseDate = toCalendar(date)
                    val title = element.getElementsByTagName("title").item(0).textContent
                    val description =
                        Jsoup.parse(element.getElementsByTagName("description").item(0).textContent).text()
                    val content =
                        Jsoup.parse(element.getElementsByTagName("content:encoded").item(0).textContent).text()
                    val link = element.getElementsByTagName("guid").item(0).textContent

                    if (this.isSameDay(calendar, releaseDate)) {
                        val news =
                            News(
                                this.getName(),
                                ISO8601.fromCalendar(releaseDate),
                                title,
                                description,
                                content,
                                link,
                                country
                            )
                        news.p = this
                        l.add(news)
                    }
                }
            }
        }

        return l.toTypedArray()
    }

    override fun getLastEpisodes(): Array<Episode> {
        val calendar = Calendar.getInstance()
        val l: MutableList<Episode> = mutableListOf()

        Country.values().filter { this.getAllowedCountries().contains(it) }.forEach { country ->
            val url: URLConnection
            val list: NodeList

            try {
                url = URL("https://www.crunchyroll.com/rss/anime?lang=${country.lang}").openConnection()
                val dbf = DocumentBuilderFactory.newInstance()
                dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                val db = dbf.newDocumentBuilder()
                val doc = db.parse(url.getInputStream())
                doc.documentElement.normalize()
                list = doc.getElementsByTagName("item")
            } catch (exception: Exception) {
                return l.toTypedArray()
            }

            for (i in 0 until list.length) {
                val node = list.item(i)

                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element

                    val date = element.getElementsByTagName("pubDate").item(0)?.textContent
                    if (date.isNullOrEmpty()) continue
                    val releaseDate = toCalendar(date)
                    val season = Const.toInt(
                        element.getElementsByTagName("crunchyroll:season").item(0)?.textContent ?: ""
                    )
                    var anime = element.getElementsByTagName("crunchyroll:seriesTitle").item(0).textContent
                    if (season.isNotEmpty()) anime += " - Saison $season"
                    var title: String? = element.getElementsByTagName("crunchyroll:episodeTitle").item(0)?.textContent
                    if (title.isNullOrEmpty()) title = null
                    val image =
                        (element.getElementsByTagName("media:thumbnail").item(0) as Element?)?.getAttribute("url")
                            ?.replace(" ", "%20") ?: ""
                    val link = element.getElementsByTagName("guid").item(0).textContent.replace(" ", "%20")
                    val number = Const.toInt(
                        element.getElementsByTagName("crunchyroll:episodeNumber").item(0)?.textContent ?: ""
                    )
                    if (number.isEmpty()) continue
                    val subtitles =
                        element.getElementsByTagName("crunchyroll:subtitleLanguages").item(0)?.textContent ?: ""
                    val spay = element.getElementsByTagName("media:restriction").item(0).textContent
                    val type =
                        if (subtitles.equals(country.language, true)) EpisodeType.DUBBED else EpisodeType.SUBTITLED
                    val id = element.getElementsByTagName("crunchyroll:mediaId").item(0).textContent

                    if (spay.split(" ").contains(country.country) && subtitles.split(",")
                            .contains(country.language) && this.isSameDay(calendar, releaseDate)
                    ) {
                        val episode = Episode(
                            platform = this.getName(),
                            calendar = ISO8601.fromCalendar(releaseDate),
                            anime = anime,
                            id = id,
                            title = title,
                            image = image,
                            link = link,
                            number = number,
                            country = country,
                            type = type
                        )
                        episode.p = this
                        l.add(episode)
                    }
                }
            }
        }

        return l.toTypedArray()
    }

    private fun toCalendar(s: String): Calendar {
        val calendar = Calendar.getInstance()
        val date = SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z", Locale.ENGLISH).parse(s)
        calendar.time = date
        return calendar
    }

    private fun isSameDay(var0: Calendar, var1: Calendar): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd")
        return fmt.format(var0.time) == fmt.format(var1.time)
    }
}