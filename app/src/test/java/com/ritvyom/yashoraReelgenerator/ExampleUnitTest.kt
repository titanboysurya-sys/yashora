package com.ritvyom.yashoraReelgenerator

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testKbsAndYonhapRssXmlParsing() {
    val kbsXml = """
      <?xml version="1.0" encoding="utf-8" ?>
      <rss version="2.0">
        <channel>
          <title><![CDATA[KBS WORLD Radio News]]></title>
          <link><![CDATA[http://world.kbs.co.kr/]]></link>
          <item>
            <title><![CDATA[S. Korea Claims 3 Golds in Fencing, Esports, Basketball with Wins over Japan]]></title>
            <link><![CDATA[https://world.kbs.co.kr/service/news_view.htm?lang=e&Seq_Code=204492]]></link>
            <description><![CDATA[South Korea swept three gold medals Saturday with victories over Japan.]]></description>
            <image><url>https://worldimg.kbs.co.kr/src/images/news/202609/260927105913_61_e.jpg</url></image>
            <pubDate>Sun, 27 Sep 2026 12:25:49 +0900</pubDate>
            <category>Sports</category>
            <enclosure url='https://worldimg.kbs.co.kr/src/images/news/202609/260927105913_61_e.jpg' type='image/jpeg' />
          </item>
        </channel>
      </rss>
    """.trimIndent()

    val repo = com.ritvyom.yashoraReelgenerator.data.repository.NewsFeedRepository()
    // Test that parseRssXml works via reflection or public method
    val method = repo.javaClass.getDeclaredMethod("parseRssXml", String::class.java, String::class.java, String::class.java, String::class.java)
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val items = method.invoke(repo, kbsXml, "KBS World", "Sports", "English") as List<com.ritvyom.yashoraReelgenerator.data.model.NewsItem>

    assertEquals(1, items.size)
    val item = items[0]
    assertEquals("S. Korea Claims 3 Golds in Fencing, Esports, Basketball with Wins over Japan", item.title)
    assertEquals("https://world.kbs.co.kr/service/news_view.htm?lang=e&Seq_Code=204492", item.link)
    assertEquals("Sports", item.category)
    assertEquals("https://worldimg.kbs.co.kr/src/images/news/202609/260927105913_61_e.jpg", item.imageUrl)
  }

  @Test
  fun testKoreaNetRssXmlParsing() {
    val koreaXml = """
      <rss version="2.0">
        <channel>
          <title>KOREA.NET</title>
          <link>https://www.korea.net</link>
          <item>
            <title><![CDATA[President Lee presents K-Initiative, vision of coexistence with NK in keynote address at UN]]></title>
            <link><![CDATA[https://www.korea.net/NewsFocus/policies/view?articleId=300130]]></link>
            <imageURL><![CDATA[https://www.korea.net/upload/content/image/aa50fdb5982a43c3932cfc84c16ca71d_20260923132931.jpg]]></imageURL>
            <description><![CDATA[President Lee Jae Myung on Sept. 22 delivers his keynote speech at the High-Level Week at the United Nations General Assembly, held annually in New York.]]></description>
            <pubDate>2026-09-23 11:10:00.0</pubDate>
            <category>Policies</category>
          </item>
        </channel>
      </rss>
    """.trimIndent()

    val repo = com.ritvyom.yashoraReelgenerator.data.repository.NewsFeedRepository()
    val method = repo.javaClass.getDeclaredMethod("parseRssXml", String::class.java, String::class.java, String::class.java, String::class.java)
    method.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val items = method.invoke(repo, koreaXml, "KOREA.NET", "Policies", "English") as List<com.ritvyom.yashoraReelgenerator.data.model.NewsItem>

    assertEquals(1, items.size)
    val item = items[0]
    assertEquals("President Lee presents K-Initiative, vision of coexistence with NK in keynote address at UN", item.title)
    assertEquals("https://www.korea.net/NewsFocus/policies/view?articleId=300130", item.link)
    assertEquals("https://www.korea.net/upload/content/image/aa50fdb5982a43c3932cfc84c16ca71d_20260923132931.jpg", item.imageUrl)
  }
}

