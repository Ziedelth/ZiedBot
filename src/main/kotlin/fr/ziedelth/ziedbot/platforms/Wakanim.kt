package fr.ziedelth.ziedbot.platforms

import fr.ziedelth.ziedbot.utils.animes.*
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.ProtocolHandshake
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.awt.Color
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class Wakanim : Platform {
    init {
        System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true")
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")
    }

    override fun getName(): String = "Wakanim"
    override fun getURL(): String = "https://www.wakanim.tv/"
    override fun getImage(): String =
        "https://play-lh.googleusercontent.com/J5_U63e4nJPrSUHeqqGIoZIaqQ1EYKEeXpcNaVbf95adUu9O9VnEgXC_ejUZPaCjpw"

    override fun getColor(): Color = Color(227, 71, 75)
    override fun getAllowedCountries(): Array<Country> = arrayOf(Country.FRANCE)

    override fun getLastNews(): Array<News> = arrayListOf<News>().toTypedArray()

    override fun getLastEpisodes(): Array<Episode> {
        val l: MutableList<Episode> = mutableListOf()
        val calendar = Calendar.getInstance()
        Logger.getLogger(ProtocolHandshake::class.java.name).level = Level.OFF

        Country.values().filter { this.getAllowedCountries().contains(it) }.forEach { country ->
            val options = FirefoxOptions()
            options.setHeadless(true)
            val driver = FirefoxDriver(options)
            val wait = WebDriverWait(driver, 60)

            try {
                driver.get("https://www.wakanim.tv/${country.country}/v2/agenda")

                val cookiesButton = aS(wait, "css-1fxzzmg")
                cookiesButton?.click()

                val privacyClose = aS(wait, "privacy-close")
                privacyClose?.click()

                val premiumEpisodes = aS(wait, "Calendar-newEp")
                premiumEpisodes?.click()

                val list = aM(wait, "Calendar-today", "Calendar-ep")
                list?.forEach {
                    val time: String? = aPS(wait, it, "Calendar-hourTxt")?.text
                    val linkElement: WebElement? = aPS(wait, it, "Calendar-linkImg")

                    if (linkElement != null) {
                        val link = linkElement.getAttribute("href")

                        if (!time.isNullOrEmpty()) {
                            val releaseDate = setDate(time.split(":")[0].toInt(), time.split(":")[1].toInt())

                            if (calendar.after(releaseDate)) {
                                val anime = aPS(wait, it, "Calendar-epTitle")!!.text
                                val image = aPS(wait, linkElement, "Calendar-image")!!.getAttribute("src")
                                val number = aPS(wait, linkElement, "Calendar-epNumber")!!.text
                                val ltype = aPS(wait, it, "Calendar-tagTranslation")!!.text

                                val episodeType =
                                    if (ltype.equals(country.voice, true)) EpisodeType.VOICE else EpisodeType.SUBTITLES
                                val id = link.replace("//", "/").split("/")[6]

                                val episode = Episode(
                                    platform = this.getName(),
                                    calendar = toStringCalendar(releaseDate),
                                    anime = anime,
                                    id = id,
                                    title = null,
                                    image = image,
                                    link = link,
                                    number = number,
                                    country = country,
                                    type = episodeType
                                )
                                episode.p = this
                                l.add(episode)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return l.toTypedArray()
            } finally {
                driver.quit()
            }
        }

        return l.toTypedArray()
    }

    private fun setDate(hour: Int, minutes: Int): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    private fun aS(wait: WebDriverWait, sClass: String): WebElement? {
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.className(sClass)))
    }

    private fun aPS(wait: WebDriverWait, parent: WebElement, sClass: String): WebElement? {
        return wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(parent, By.className(sClass)))
    }

    private fun aM(wait: WebDriverWait, parent: String, sClass: String): List<WebElement>? {
        return wait.until(
            ExpectedConditions.presenceOfNestedElementsLocatedBy(
                By.className(parent),
                By.className(sClass)
            )
        )
    }
}