import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class swiftle extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color FIELD = new Color(0x241240);
    static final Color BORDER = new Color(0x4c1d95);
    static final Color EXACT = new Color(0xa855f7);
    static final Color CLOSE = new Color(0xec4899);
    static final Color MISS = new Color(0x2c2140);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x7c3aed);
    static final Color BTN_HOVER = new Color(0x9b6bff);
    static final String FONT = "SansSerif";

    static final Map<String, Color> ERA = new HashMap<>();
    static {
        ERA.put("debut", new Color(0x1D4737));
        ERA.put("fearless", new Color(0xCBA863));
        ERA.put("speaknow", new Color(0x72325F));
        ERA.put("red", new Color(0xA91E47));
        ERA.put("1989", new Color(0x8BB5D2));
        ERA.put("reputation", new Color(0x2C2C2C));
        ERA.put("lover", new Color(0xEBBED3));
        ERA.put("folklore", new Color(0x949494));
        ERA.put("evermore", new Color(0x6B3324));
        ERA.put("midnights", new Color(0x5A658B));
        ERA.put("ttpd", new Color(0xADA795));
        ERA.put("showgirl", new Color(0xC44615));
        ERA.put("holiday", new Color(0x7A1F2B));
        ERA.put("singles", new Color(0x5B3A8F));
        ERA.put("collabs", new Color(0x6CAE90));
    }

    static Color textOn(Color bg) {
        double l = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255;
        return l > 0.55 ? new Color(0x1a0b2e) : Color.WHITE;
    }

    static class Song {
        final String title, album, era, feat, type;
        final int track, sec, year;
        final Set<String> feats = new HashSet<>();

        Song(String line) {
            String[] p = line.split("\\|", -1);
            title = p[0]; album = p[1]; era = p[2];
            track = Integer.parseInt(p[3]); sec = Integer.parseInt(p[4]);
            feat = p[5]; year = Integer.parseInt(p[6]); type = p[7];
            for (String f : feat.split(",")) if (!f.isBlank()) feats.add(f.trim().toLowerCase());
        }

        String length() { return sec / 60 + ":" + String.format("%02d", sec % 60); }
        String trackLabel() { return track == 0 ? "—" : "#" + track; }
        String featLabel() { return feat.isEmpty() ? "None" : feat; }
        boolean easy() { return type.equals("Standard"); }
    }

    static final String[] DATA = {
        "Tim McGraw|Taylor Swift|debut|1|232||2006|Standard",
        "Picture To Burn|Taylor Swift|debut|2|173||2006|Standard",
        "Teardrops On My Guitar|Taylor Swift|debut|3|203||2006|Standard",
        "A Place In This World|Taylor Swift|debut|4|199||2006|Standard",
        "Cold As You|Taylor Swift|debut|5|239||2006|Standard",
        "The Outside|Taylor Swift|debut|6|207||2006|Standard",
        "Tied Together With A Smile|Taylor Swift|debut|7|248||2006|Standard",
        "Stay Beautiful|Taylor Swift|debut|8|236||2006|Standard",
        "Should've Said No|Taylor Swift|debut|9|242||2006|Standard",
        "Mary's Song (Oh My My My)|Taylor Swift|debut|10|213||2006|Standard",
        "Our Song|Taylor Swift|debut|11|201||2006|Standard",
        "I'm Only Me When I'm With You|Taylor Swift|debut|12|213||2006|Deluxe",
        "Invisible|Taylor Swift|debut|13|203||2006|Deluxe",
        "A Perfectly Good Heart|Taylor Swift|debut|14|220||2006|Deluxe",
        "Fearless|Fearless|fearless|1|242||2008|Standard",
        "Fifteen|Fearless|fearless|2|294||2008|Standard",
        "Love Story|Fearless|fearless|3|236||2008|Standard",
        "Hey Stephen|Fearless|fearless|4|254||2008|Standard",
        "White Horse|Fearless|fearless|5|235||2008|Standard",
        "You Belong With Me|Fearless|fearless|6|231||2008|Standard",
        "Breathe|Fearless|fearless|7|263|Colbie Caillat|2008|Standard",
        "Tell Me Why|Fearless|fearless|8|201||2008|Standard",
        "You're Not Sorry|Fearless|fearless|9|262||2008|Standard",
        "The Way I Loved You|Fearless|fearless|10|243||2008|Standard",
        "Forever & Always|Fearless|fearless|11|225||2008|Standard",
        "The Best Day|Fearless|fearless|12|245||2008|Standard",
        "Change|Fearless|fearless|13|279||2008|Standard",
        "Jump Then Fall|Fearless|fearless|14|238||2008|Deluxe",
        "Untouchable|Fearless|fearless|15|312||2008|Deluxe",
        "Come In With The Rain|Fearless|fearless|17|237||2008|Deluxe",
        "SuperStar|Fearless|fearless|18|264||2008|Deluxe",
        "The Other Side Of The Door|Fearless|fearless|19|238||2008|Deluxe",
        "Today Was A Fairytale|Fearless|fearless|20|242||2008|Deluxe",
        "You All Over Me|Fearless|fearless|21|221|Maren Morris|2008|Vault",
        "Mr. Perfectly Fine|Fearless|fearless|22|278||2008|Vault",
        "We Were Happy|Fearless|fearless|23|244||2008|Vault",
        "That's When|Fearless|fearless|24|189|Keith Urban|2008|Vault",
        "Don't You|Fearless|fearless|25|209||2008|Vault",
        "Bye Bye Baby|Fearless|fearless|26|242||2008|Vault",
        "Mine|Speak Now|speaknow|1|232||2010|Standard",
        "Sparks Fly|Speak Now|speaknow|2|261||2010|Standard",
        "Back To December|Speak Now|speaknow|3|294||2010|Standard",
        "Speak Now|Speak Now|speaknow|4|242||2010|Standard",
        "Dear John|Speak Now|speaknow|5|406||2010|Standard",
        "Mean|Speak Now|speaknow|6|239||2010|Standard",
        "The Story Of Us|Speak Now|speaknow|7|268||2010|Standard",
        "Never Grow Up|Speak Now|speaknow|8|293||2010|Standard",
        "Enchanted|Speak Now|speaknow|9|353||2010|Standard",
        "Better Than Revenge|Speak Now|speaknow|10|220||2010|Standard",
        "Innocent|Speak Now|speaknow|11|301||2010|Standard",
        "Haunted|Speak Now|speaknow|12|245||2010|Standard",
        "Last Kiss|Speak Now|speaknow|13|369||2010|Standard",
        "Long Live|Speak Now|speaknow|14|318||2010|Standard",
        "Ours|Speak Now|speaknow|15|236||2010|Deluxe",
        "Superman|Speak Now|speaknow|16|275||2010|Deluxe",
        "Electric Touch|Speak Now|speaknow|17|266|Fall Out Boy|2010|Vault",
        "When Emma Falls In Love|Speak Now|speaknow|18|252||2010|Vault",
        "I Can See You|Speak Now|speaknow|19|273||2010|Vault",
        "Castles Crumbling|Speak Now|speaknow|20|306|Hayley Williams|2010|Vault",
        "Foolish One|Speak Now|speaknow|21|312||2010|Vault",
        "Timeless|Speak Now|speaknow|22|321||2010|Vault",
        "State Of Grace|Red|red|1|295||2012|Standard",
        "Red|Red|red|2|223||2012|Standard",
        "Treacherous|Red|red|3|243||2012|Standard",
        "I Knew You Were Trouble|Red|red|4|220||2012|Standard",
        "All Too Well|Red|red|5|329||2012|Standard",
        "22|Red|red|6|231||2012|Standard",
        "I Almost Do|Red|red|7|245||2012|Standard",
        "We Are Never Ever Getting Back Together|Red|red|8|193||2012|Standard",
        "Stay Stay Stay|Red|red|9|206||2012|Standard",
        "The Last Time|Red|red|10|299|Gary Lightbody|2012|Standard",
        "Holy Ground|Red|red|11|203||2012|Standard",
        "Sad Beautiful Tragic|Red|red|12|285||2012|Standard",
        "The Lucky One|Red|red|13|240||2012|Standard",
        "Everything Has Changed|Red|red|14|245|Ed Sheeran|2012|Standard",
        "Starlight|Red|red|15|220||2012|Standard",
        "Begin Again|Red|red|16|239||2012|Standard",
        "The Moment I Knew|Red|red|17|285||2012|Deluxe",
        "Come Back...Be Here|Red|red|18|223||2012|Deluxe",
        "Girl At Home|Red|red|19|221||2012|Deluxe",
        "Ronan|Red|red|21|264||2012|Deluxe",
        "Better Man|Red|red|22|297||2012|Vault",
        "Nothing New|Red|red|23|259|Phoebe Bridgers|2012|Vault",
        "Babe|Red|red|24|224||2012|Vault",
        "Message In A Bottle|Red|red|25|226||2012|Vault",
        "I Bet You Think About Me|Red|red|26|285|Chris Stapleton|2012|Vault",
        "Forever Winter|Red|red|27|264||2012|Vault",
        "Run|Red|red|28|240|Ed Sheeran|2012|Vault",
        "The Very First Night|Red|red|29|200||2012|Vault",
        "All Too Well (10 Minute Version)|Red|red|30|613||2012|Vault",
        "Welcome To New York|1989|1989|1|213||2014|Standard",
        "Blank Space|1989|1989|2|232||2014|Standard",
        "Style|1989|1989|3|231||2014|Standard",
        "Out Of The Woods|1989|1989|4|236||2014|Standard",
        "All You Had To Do Was Stay|1989|1989|5|193||2014|Standard",
        "Shake It Off|1989|1989|6|219||2014|Standard",
        "I Wish You Would|1989|1989|7|208||2014|Standard",
        "Bad Blood|1989|1989|8|211||2014|Standard",
        "Wildest Dreams|1989|1989|9|220||2014|Standard",
        "How You Get The Girl|1989|1989|10|248||2014|Standard",
        "This Love|1989|1989|11|250||2014|Standard",
        "I Know Places|1989|1989|12|196||2014|Standard",
        "Clean|1989|1989|13|271||2014|Standard",
        "Wonderland|1989|1989|14|246||2014|Deluxe",
        "You Are In Love|1989|1989|15|267||2014|Deluxe",
        "New Romantics|1989|1989|16|230||2014|Deluxe",
        "\"Slut!\"|1989|1989|17|180||2014|Vault",
        "Say Don't Go|1989|1989|18|280||2014|Vault",
        "Now That We Don't Talk|1989|1989|19|146||2014|Vault",
        "Suburban Legends|1989|1989|20|172||2014|Vault",
        "Is It Over Now?|1989|1989|21|229||2014|Vault",
        "...Ready For It?|reputation|reputation|1|208||2017|Standard",
        "End Game|reputation|reputation|2|245|Ed Sheeran, Future|2017|Standard",
        "I Did Something Bad|reputation|reputation|3|238||2017|Standard",
        "Don't Blame Me|reputation|reputation|4|236||2017|Standard",
        "Delicate|reputation|reputation|5|232||2017|Standard",
        "Look What You Made Me Do|reputation|reputation|6|212||2017|Standard",
        "So It Goes...|reputation|reputation|7|228||2017|Standard",
        "Gorgeous|reputation|reputation|8|210||2017|Standard",
        "Getaway Car|reputation|reputation|9|234||2017|Standard",
        "King Of My Heart|reputation|reputation|10|214||2017|Standard",
        "Dancing With Our Hands Tied|reputation|reputation|11|212||2017|Standard",
        "Dress|reputation|reputation|12|230||2017|Standard",
        "This Is Why We Can't Have Nice Things|reputation|reputation|13|207||2017|Standard",
        "Call It What You Want|reputation|reputation|14|204||2017|Standard",
        "New Year's Day|reputation|reputation|15|235||2017|Standard",
        "I Forgot That You Existed|Lover|lover|1|171||2019|Standard",
        "Cruel Summer|Lover|lover|2|178||2019|Standard",
        "Lover|Lover|lover|3|221||2019|Standard",
        "The Man|Lover|lover|4|190||2019|Standard",
        "The Archer|Lover|lover|5|211||2019|Standard",
        "I Think He Knows|Lover|lover|6|173||2019|Standard",
        "Miss Americana & The Heartbreak Prince|Lover|lover|7|234||2019|Standard",
        "Paper Rings|Lover|lover|8|222||2019|Standard",
        "Cornelia Street|Lover|lover|9|287||2019|Standard",
        "Death By A Thousand Cuts|Lover|lover|10|199||2019|Standard",
        "London Boy|Lover|lover|11|190||2019|Standard",
        "Soon You'll Get Better|Lover|lover|12|202|The Chicks|2019|Standard",
        "False God|Lover|lover|13|200||2019|Standard",
        "You Need To Calm Down|Lover|lover|14|171||2019|Standard",
        "Afterglow|Lover|lover|15|223||2019|Standard",
        "ME!|Lover|lover|16|193|Brendon Urie|2019|Standard",
        "It's Nice To Have A Friend|Lover|lover|17|150||2019|Standard",
        "Daylight|Lover|lover|18|293||2019|Standard",
        "the 1|folklore|folklore|1|210||2020|Standard",
        "cardigan|folklore|folklore|2|240||2020|Standard",
        "the last great american dynasty|folklore|folklore|3|231||2020|Standard",
        "exile|folklore|folklore|4|286|Bon Iver|2020|Standard",
        "my tears ricochet|folklore|folklore|5|256||2020|Standard",
        "mirrorball|folklore|folklore|6|209||2020|Standard",
        "seven|folklore|folklore|7|209||2020|Standard",
        "august|folklore|folklore|8|262||2020|Standard",
        "this is me trying|folklore|folklore|9|195||2020|Standard",
        "illicit affairs|folklore|folklore|10|191||2020|Standard",
        "invisible string|folklore|folklore|11|253||2020|Standard",
        "mad woman|folklore|folklore|12|237||2020|Standard",
        "epiphany|folklore|folklore|13|290||2020|Standard",
        "betty|folklore|folklore|14|295||2020|Standard",
        "peace|folklore|folklore|15|234||2020|Standard",
        "hoax|folklore|folklore|16|220||2020|Standard",
        "the lakes|folklore|folklore|17|212||2020|Deluxe",
        "willow|evermore|evermore|1|215||2020|Standard",
        "champagne problems|evermore|evermore|2|244||2020|Standard",
        "gold rush|evermore|evermore|3|185||2020|Standard",
        "'tis the damn season|evermore|evermore|4|230||2020|Standard",
        "tolerate it|evermore|evermore|5|245||2020|Standard",
        "no body, no crime|evermore|evermore|6|216|HAIM|2020|Standard",
        "happiness|evermore|evermore|7|315||2020|Standard",
        "dorothea|evermore|evermore|8|226||2020|Standard",
        "coney island|evermore|evermore|9|275|The National|2020|Standard",
        "ivy|evermore|evermore|10|260||2020|Standard",
        "cowboy like me|evermore|evermore|11|275||2020|Standard",
        "long story short|evermore|evermore|12|216||2020|Standard",
        "marjorie|evermore|evermore|13|258||2020|Standard",
        "closure|evermore|evermore|14|181||2020|Standard",
        "evermore|evermore|evermore|15|304|Bon Iver|2020|Standard",
        "right where you left me|evermore|evermore|16|245||2020|Deluxe",
        "it's time to go|evermore|evermore|17|255||2020|Deluxe",
        "Lavender Haze|Midnights|midnights|1|202||2022|Standard",
        "Maroon|Midnights|midnights|2|218||2022|Standard",
        "Anti-Hero|Midnights|midnights|3|201||2022|Standard",
        "Snow On The Beach|Midnights|midnights|4|256|Lana Del Rey|2022|Standard",
        "You're On Your Own, Kid|Midnights|midnights|5|194||2022|Standard",
        "Midnight Rain|Midnights|midnights|6|175||2022|Standard",
        "Question...?|Midnights|midnights|7|211||2022|Standard",
        "Vigilante Shit|Midnights|midnights|8|165||2022|Standard",
        "Bejeweled|Midnights|midnights|9|194||2022|Standard",
        "Labyrinth|Midnights|midnights|10|248||2022|Standard",
        "Karma|Midnights|midnights|11|205||2022|Standard",
        "Sweet Nothing|Midnights|midnights|12|188||2022|Standard",
        "Mastermind|Midnights|midnights|13|191||2022|Standard",
        "The Great War|Midnights|midnights|14|240||2022|Deluxe",
        "Hits Different|Midnights|midnights|21|234||2022|Deluxe",
        "Bigger Than The Whole Sky|Midnights|midnights|15|219||2022|Deluxe",
        "Paris|Midnights|midnights|16|196||2022|Deluxe",
        "High Infidelity|Midnights|midnights|17|231||2022|Deluxe",
        "Glitch|Midnights|midnights|18|149||2022|Deluxe",
        "Would've, Could've, Should've|Midnights|midnights|19|260||2022|Deluxe",
        "You're Losing Me|Midnights|midnights|19|278||2022|Vault",
        "Dear Reader|Midnights|midnights|20|225||2022|Deluxe",
        "Fortnight|TTPD|ttpd|1|229|Post Malone|2024|Standard",
        "The Tortured Poets Department|TTPD|ttpd|2|293||2024|Standard",
        "My Boy Only Breaks His Favorite Toys|TTPD|ttpd|3|204||2024|Standard",
        "Down Bad|TTPD|ttpd|4|261||2024|Standard",
        "So Long, London|TTPD|ttpd|5|263||2024|Standard",
        "But Daddy I Love Him|TTPD|ttpd|6|340||2024|Standard",
        "Fresh Out The Slammer|TTPD|ttpd|7|211||2024|Standard",
        "Florida!!!|TTPD|ttpd|8|215|Florence + The Machine|2024|Standard",
        "Guilty As Sin?|TTPD|ttpd|9|254||2024|Standard",
        "Who's Afraid Of Little Old Me?|TTPD|ttpd|10|334||2024|Standard",
        "I Can Fix Him (No Really I Can)|TTPD|ttpd|11|156||2024|Standard",
        "loml|TTPD|ttpd|12|277||2024|Standard",
        "I Can Do It With A Broken Heart|TTPD|ttpd|13|218||2024|Standard",
        "The Smallest Man Who Ever Lived|TTPD|ttpd|14|246||2024|Standard",
        "The Alchemy|TTPD|ttpd|15|197||2024|Standard",
        "Clara Bow|TTPD|ttpd|16|217||2024|Standard",
        "The Black Dog|TTPD|ttpd|17|239||2024|Deluxe",
        "imgonnagetyouback|TTPD|ttpd|18|222||2024|Deluxe",
        "The Albatross|TTPD|ttpd|19|184||2024|Deluxe",
        "Chloe Or Sam Or Sophia Or Marcus|TTPD|ttpd|20|213||2024|Deluxe",
        "How Did It End?|TTPD|ttpd|21|239||2024|Deluxe",
        "So High School|TTPD|ttpd|22|229||2024|Deluxe",
        "I Hate It Here|TTPD|ttpd|23|244||2024|Deluxe",
        "thanK you aIMee|TTPD|ttpd|24|264||2024|Deluxe",
        "I Look In People's Windows|TTPD|ttpd|25|132||2024|Deluxe",
        "The Prophecy|TTPD|ttpd|26|250||2024|Deluxe",
        "Cassandra|TTPD|ttpd|27|240||2024|Deluxe",
        "Peter|TTPD|ttpd|28|284||2024|Deluxe",
        "The Bolter|TTPD|ttpd|29|238||2024|Deluxe",
        "Robin|TTPD|ttpd|30|241||2024|Deluxe",
        "The Manuscript|TTPD|ttpd|31|225||2024|Deluxe",
        "The Fate Of Ophelia|The Life of a Showgirl|showgirl|1|226||2025|Standard",
        "Elizabeth Taylor|The Life of a Showgirl|showgirl|2|208||2025|Standard",
        "Opalite|The Life of a Showgirl|showgirl|3|235||2025|Standard",
        "Father Figure|The Life of a Showgirl|showgirl|4|213||2025|Standard",
        "Eldest Daughter|The Life of a Showgirl|showgirl|5|246||2025|Standard",
        "Ruin The Friendship|The Life of a Showgirl|showgirl|6|221||2025|Standard",
        "Actually Romantic|The Life of a Showgirl|showgirl|7|164||2025|Standard",
        "Wi$h Li$t|The Life of a Showgirl|showgirl|8|207||2025|Standard",
        "Wood|The Life of a Showgirl|showgirl|9|151||2025|Standard",
        "CANCELLED!|The Life of a Showgirl|showgirl|10|211||2025|Standard",
        "Honey|The Life of a Showgirl|showgirl|11|182||2025|Standard",
        "The Life Of A Showgirl|The Life of a Showgirl|showgirl|12|242|Sabrina Carpenter|2025|Standard",
        "If This Was A Movie|Speak Now|speaknow|16|237||2010|Deluxe",
        "Last Christmas|Holiday Collection|holiday|1|208||2007|Holiday",
        "Christmases When You Were Mine|Holiday Collection|holiday|2|184||2007|Holiday",
        "Santa Baby|Holiday Collection|holiday|3|159||2007|Holiday",
        "Silent Night|Holiday Collection|holiday|4|210||2007|Holiday",
        "Christmas Must Be Something More|Holiday Collection|holiday|5|232||2007|Holiday",
        "White Christmas|Holiday Collection|holiday|6|154||2007|Holiday",
        "Beautiful Eyes|Beautiful Eyes|debut|1|178||2008|EP",
        "I Heart ?|Beautiful Eyes|debut|6|195||2008|EP",
        "Eyes Open|The Hunger Games|singles|0|243||2012|Single",
        "Safe & Sound|The Hunger Games|singles|0|240|Joy Williams, John Paul White|2012|Single",
        "Crazier|Hannah Montana: The Movie|singles|0|192||2009|Single",
        "Sweeter Than Fiction|One Chance|singles|0|238||2013|Single",
        "Beautiful Ghosts|Cats|singles|0|261||2019|Single",
        "Christmas Tree Farm|Single|singles|0|228||2019|Single",
        "Only The Young|Miss Americana|singles|0|158||2020|Single",
        "Carolina|Where the Crawdads Sing|singles|0|264||2022|Single",
        "All Of The Girls You Loved Before|Single|singles|0|221||2023|Single",
        "September|Spotify Singles|singles|0|188||2018|Single",
        "I Don't Wanna Live Forever|Fifty Shades Darker|collabs|0|246|ZAYN|2016|Collab",
        "Two Is Better Than One|Love Drunk|collabs|0|243|Boys Like Girls|2009|Collab",
        "Both Of Us|Strange Clouds|collabs|0|216|B.o.B|2012|Collab",
        "Highway Don't Care|Two Lanes of Freedom|collabs|0|277|Tim McGraw, Keith Urban|2013|Collab",
        "Gasoline|Women In Music Pt. III|collabs|0|193|HAIM|2020|Collab",
        "Renegade|How Long Do You Think It's Gonna Last?|collabs|0|254|Big Red Machine|2021|Collab",
        "Birch|How Long Do You Think It's Gonna Last?|collabs|0|330|Big Red Machine|2021|Collab",
        "The Joker And The Queen|= (Ed Sheeran)|collabs|0|185|Ed Sheeran|2022|Collab",
        "The Alcott|First Two Pages of Frankenstein|collabs|0|268|The National|2023|Collab",
        "us.|The Secret of Us|collabs|0|243|Gracie Abrams|2024|Collab",
        "Half Of My Heart|Battle Studies|collabs|0|250|John Mayer|2010|Collab"
    };
    static final List<Song> SONGS = new ArrayList<>();
    static { for (String s : DATA) SONGS.add(new Song(s)); }

    static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    static final int EX = 2, CL = 1, NO = 0;
    static final String[] COLS = {"Album", "Year", "Track", "Length", "Feat.", "Type"};

    static class Cell {
        final String text; final int state; final int dir;
        Cell(String t, int s, int d) { text = t; state = s; dir = d; }
    }

    static Cell num(String label, int g, int a, int closeWithin, boolean naZero) {
        if (naZero && (g == 0 || a == 0)) return new Cell(label, g == a ? EX : NO, 0);
        if (g == a) return new Cell(label, EX, 0);
        return new Cell(label, Math.abs(g - a) <= closeWithin ? CL : NO, a > g ? 1 : -1);
    }

    static Cell[] compare(Song g, Song a) {
        Cell[] c = new Cell[6];
        c[0] = new Cell(g.album, g.album.equals(a.album) ? EX : NO, 0);
        c[1] = num(String.valueOf(g.year), g.year, a.year, 2, false);
        c[2] = num(g.trackLabel(), g.track, a.track, 2, true);
        c[3] = num(g.length(), g.sec, a.sec, 20, false);
        int fs;
        if (g.feats.equals(a.feats)) fs = EX;
        else { Set<String> s = new HashSet<>(g.feats); s.retainAll(a.feats); fs = s.isEmpty() ? NO : CL; }
        c[4] = new Cell(g.featLabel(), fs, 0);
        c[5] = new Cell(g.type, g.type.equals(a.type) ? EX : NO, 0);
        return c;
    }

    static final int W = 560, H = 680, MAX_GUESSES = 8;

    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    final List<Song> guesses = new ArrayList<>();
    Song answer;
    boolean over = false, won = false;
    boolean easyMode = false;
    int played, wins, streak, best;

    final JTextField field = new JTextField();
    final DefaultListModel<Song> sugModel = new DefaultListModel<>();
    final JList<Song> sugList = new JList<>(sugModel);
    final JScrollPane sugScroll = new JScrollPane(sugList);
    final Board board = new Board();
    final JLabel counter = new JLabel();
    final JLabel streakLbl = new JLabel();
    final Overlay overlay = new Overlay();
    final Pill giveUp = new Pill("Give up", MISS, 12, this::giveUp);

    public swiftle() {
        super("Swiftle");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        loadStats();

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        root.setPreferredSize(new Dimension(W, H));
        setContentPane(root);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("<html><span style='color:#c084fc'>Swift</span><span style='color:#f3e8ff'>le</span></html>");
        title.setFont(new Font(FONT, Font.BOLD, 24));
        head.add(title, BorderLayout.WEST);
        streakLbl.setForeground(MUTED);
        streakLbl.setFont(new Font(FONT, Font.BOLD, 12));
        head.add(streakLbl, BorderLayout.EAST);

        field.setFont(new Font(FONT, Font.PLAIN, 15));
        field.setBackground(FIELD);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2, true), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(field, BorderLayout.CENTER);
        searchRow.add(giveUp, BorderLayout.EAST);

        JPanel info = new JPanel(new BorderLayout());
        info.setOpaque(false);
        counter.setForeground(MUTED);
        counter.setFont(new Font(FONT, Font.PLAIN, 12));
        info.add(counter, BorderLayout.WEST);
        JLabel legend = new JLabel("<html><span style='color:#a855f7'>■</span> exact &nbsp; <span style='color:#ec4899'>■</span> close &nbsp; ↑↓ answer is higher/lower</html>");
        legend.setForeground(MUTED);
        legend.setFont(new Font(FONT, Font.PLAIN, 11));
        info.add(legend, BorderLayout.EAST);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{head, searchRow, info}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            top.add(c);
            top.add(Box.createVerticalStrut(8));
        }
        root.add(top, BorderLayout.NORTH);

        JScrollPane boardScroll = new JScrollPane(board);
        boardScroll.setBorder(null);
        boardScroll.getViewport().setBackground(BG);
        boardScroll.getVerticalScrollBar().setUnitIncrement(16);
        purpleScroll(boardScroll);
        root.add(boardScroll, BorderLayout.CENTER);

        sugList.setBackground(PANEL);
        sugList.setForeground(TEXT);
        sugList.setFont(new Font(FONT, Font.PLAIN, 14));
        sugList.setSelectionBackground(BTN);
        sugList.setSelectionForeground(TEXT);
        sugList.setFixedCellHeight(30);
        sugList.setCellRenderer(new SongCell());
        sugScroll.setBorder(BorderFactory.createLineBorder(BORDER, 2));
        sugScroll.setVisible(false);
        getLayeredPane().add(sugScroll, JLayeredPane.POPUP_LAYER);

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });
        field.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int n = sugModel.size();
                if (e.getKeyCode() == KeyEvent.VK_DOWN && n > 0) {
                    sugList.setSelectedIndex(Math.min(n - 1, sugList.getSelectedIndex() + 1));
                    sugList.ensureIndexIsVisible(sugList.getSelectedIndex());
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP && n > 0) {
                    sugList.setSelectedIndex(Math.max(0, sugList.getSelectedIndex() - 1));
                    sugList.ensureIndexIsVisible(sugList.getSelectedIndex());
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (sugScroll.isVisible() && sugList.getSelectedValue() != null) guess(sugList.getSelectedValue());
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    sugScroll.setVisible(false);
                }
            }
        });
        sugList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int i = sugList.locationToIndex(e.getPoint());
                if (i >= 0) guess(sugModel.get(i));
            }
        });

        setGlassPane(overlay);
        pack();
        setLocationRelativeTo(null);
        overlay.showStart();
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("swiftle-diva"); } catch (Exception e) { return null; }
    }

    void loadStats() {
        if (prefs == null) return;
        played = prefs.getInt("played", 0); wins = prefs.getInt("wins", 0);
        streak = prefs.getInt("streak", 0); best = prefs.getInt("best", 0);
    }

    void saveStats() {
        if (prefs == null) return;
        try {
            prefs.putInt("played", played); prefs.putInt("wins", wins);
            prefs.putInt("streak", streak); prefs.putInt("best", best);
        } catch (Exception ignored) {}
    }

    void newSong(boolean easy) {
        easyMode = easy;
        List<Song> pool = new ArrayList<>();
        for (Song s : SONGS) if (!easy || s.easy()) pool.add(s);
        Song next;
        do { next = pool.get(rng.nextInt(pool.size())); } while (next == answer && pool.size() > 1);
        answer = next;
        guesses.clear();
        over = false; won = false;
        field.setText("");
        field.setEnabled(true);
        giveUp.setVisible(true);
        refresh();
        overlay.setVisible(false);
        field.requestFocusInWindow();
    }

    void updateSuggestions() {
        String q = norm(field.getText());
        sugModel.clear();
        if (q.isEmpty() || over) { sugScroll.setVisible(false); return; }
        List<Song> starts = new ArrayList<>(), contains = new ArrayList<>();
        for (Song s : SONGS) {
            if (guesses.contains(s)) continue;
            String t = norm(s.title);
            if (t.startsWith(q)) starts.add(s);
            else if (t.contains(q) || norm(s.album).startsWith(q)) contains.add(s);
        }
        starts.addAll(contains);
        for (Song s : starts) sugModel.addElement(s);
        if (sugModel.isEmpty()) { sugScroll.setVisible(false); return; }
        sugList.setSelectedIndex(0);
        Point p = SwingUtilities.convertPoint(field.getParent(), field.getLocation(), getLayeredPane());
        int rows = Math.min(7, sugModel.size());
        sugScroll.setBounds(p.x, p.y + field.getHeight() + 2, field.getWidth(), rows * 30 + 4);
        sugScroll.setVisible(true);
        sugScroll.revalidate();
    }

    void guess(Song s) {
        if (over || s == null || guesses.contains(s)) return;
        guesses.add(0, s);
        field.setText("");
        sugScroll.setVisible(false);
        if (s == answer) finish(true);
        else if (guesses.size() >= MAX_GUESSES) finish(false);
        refresh();
    }

    void giveUp() {
        if (!over) { finish(false); refresh(); }
    }

    void finish(boolean win) {
        over = true; won = win;
        played++;
        if (win) { wins++; streak++; best = Math.max(best, streak); } else streak = 0;
        saveStats();
        field.setEnabled(false);
        giveUp.setVisible(false);
        javax.swing.Timer t = new javax.swing.Timer(win ? 700 : 400, e -> overlay.showEnd());
        t.setRepeats(false);
        t.start();
    }

    void refresh() {
        String c = over ? (won ? "Solved in " + guesses.size() : "Out of guesses") : "Guess " + (guesses.size() + 1) + " of " + MAX_GUESSES;
        counter.setText(c
                + (easyMode ? "  ·  Easy: standard album tracks" : "  ·  Swiftie: all " + SONGS.size() + " songs"));
        streakLbl.setText("streak " + streak + "  ·  best " + best);
        board.revalidate();
        board.repaint();
    }

    static final int ROW_H = 92, TILE_W = 82, TILE_H = 50, TILE_GAP = 4;

    class Board extends JComponent {
        @Override public Dimension getPreferredSize() {
            return new Dimension(W - 44, Math.max(1, guesses.size()) * (ROW_H + 8) + 30);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            int w = getWidth();

            if (guesses.isEmpty()) {
                g.setColor(MUTED);
                g.setFont(new Font(FONT, Font.PLAIN, 14));
                String[] lines = {"Start typing a song title above", "",
                        "Each guess shows how close you are on", "album, year, track number, length,",
                        "featured artists and song type."};
                int y = 60;
                for (String l : lines) {
                    g.drawString(l, (w - g.getFontMetrics().stringWidth(l)) / 2, y);
                    y += 22;
                }
                g.dispose();
                return;
            }

            int y = 0;
            for (Song s : guesses) {
                Cell[] cells = compare(s, answer);
                Color era = ERA.get(s.era);
                if ((era.getRed() + era.getGreen() + era.getBlue()) / 3 < 70) era = new Color(0x77707f);
                g.setColor(PANEL);
                g.fillRoundRect(0, y, w - 1, ROW_H, 14, 14);
                g.setColor(era);
                g.fillRoundRect(0, y, 8, ROW_H, 8, 8);
                g.fillRect(4, y, 4, ROW_H);
                g.setFont(new Font(FONT, Font.BOLD, 15));
                g.setColor(s == answer ? EXACT : TEXT);
                String t = s.title;
                FontMetrics fm = g.getFontMetrics();
                while (fm.stringWidth(t) > w - 40 && t.length() > 4) t = t.substring(0, t.length() - 2);
                if (!t.equals(s.title)) t = t + "…";
                g.drawString(t, 18, y + 22);
                int tx = 14;
                for (int i = 0; i < 6; i++) {
                    tile(g, tx, y + 32, COLS[i], cells[i]);
                    tx += TILE_W + TILE_GAP;
                }
                y += ROW_H + 8;
            }
            g.dispose();
        }

        void tile(Graphics2D g, int x, int y, String head, Cell c) {
            g.setColor(c.state == EX ? EXACT : c.state == CL ? CLOSE : MISS);
            g.fillRoundRect(x, y, TILE_W, TILE_H, 10, 10);
            g.setFont(new Font(FONT, Font.BOLD, 9));
            g.setColor(new Color(255, 255, 255, 170));
            g.drawString(head.toUpperCase(), x + 7, y + 13);
            String txt = c.text + (c.dir > 0 ? " ↑" : c.dir < 0 ? " ↓" : "");
            g.setColor(Color.WHITE);
            Font f = new Font(FONT, Font.BOLD, 13);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(txt) > TILE_W - 10) {
                g.setFont(new Font(FONT, Font.BOLD, 10));
                fm = g.getFontMetrics();
                List<String> lines = wrap(txt, fm, TILE_W - 10, 2);
                int ly = y + 28;
                for (String l : lines) { g.drawString(l, x + 6, ly); ly += 12; }
            } else {
                g.drawString(txt, x + 7, y + 36);
            }
        }
    }

    static List<String> wrap(String s, FontMetrics fm, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : s.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) <= width) line = test;
            else { if (!line.isEmpty()) out.add(line); line = word; }
        }
        if (!line.isEmpty()) out.add(line);
        if (out.size() > maxLines) {
            out = new ArrayList<>(out.subList(0, maxLines));
            String last = out.get(maxLines - 1);
            while (fm.stringWidth(last + "…") > width && last.length() > 1) last = last.substring(0, last.length() - 1);
            out.set(maxLines - 1, last + "…");
        }
        for (int i = 0; i < out.size(); i++) {
            String l = out.get(i);
            while (fm.stringWidth(l) > width && l.length() > 1) l = l.substring(0, l.length() - 1);
            out.set(i, l);
        }
        return out;
    }

    class SongCell extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
            Song s = (Song) value;
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
            String hex = String.format("#%06x", ERA.get(s.era).getRGB() & 0xffffff);
            l.setText("<html><span style='color:" + hex + "'>●</span>&nbsp; " + esc(s.title)
                    + " <span style='color:#a78bca;font-size:10px'>&nbsp;" + esc(s.album) + "</span></html>");
            l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            l.setBackground(sel ? BTN : PANEL);
            l.setForeground(TEXT);
            return l;
        }
    }

    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }

    static class Pill extends JComponent {
        String text; Color base; boolean hover;
        Pill(String text, Color base, int size, Runnable action) {
            this.text = text; this.base = base;
            setFont(new Font(FONT, Font.BOLD, size));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(fm.stringWidth(text) + 28, fm.getHeight() + 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) action.run(); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = base;
            if (hover) c = new Color(Math.min(255, c.getRed() + 25), Math.min(255, c.getGreen() + 25), Math.min(255, c.getBlue() + 25));
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g.setColor(textOn(base));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }

    class Overlay extends JPanel {
        final JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(PANEL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(BORDER);
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                g.dispose();
            }
        };

        Overlay() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
            add(card);
            addMouseListener(new MouseAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
            addKeyListener(new KeyAdapter() {});
        }

        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(20, 8, 36, 220));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        JLabel lbl(String html, int size, Color c, boolean bold) {
            JLabel l = new JLabel("<html><div style='text-align:center'>" + html + "</div></html>");
            l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
            l.setForeground(c);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setAlignmentX(CENTER_ALIGNMENT);
            return l;
        }

        void add(JComponent c, int gapAfter) {
            c.setAlignmentX(CENTER_ALIGNMENT);
            card.add(c);
            if (gapAfter > 0) card.add(Box.createVerticalStrut(gapAfter));
        }

        Pill mode(String text, Color c, boolean easy) {
            Pill p = new Pill(text, c, 14, () -> newSong(easy));
            p.setMaximumSize(new Dimension(280, 42));
            p.setPreferredSize(new Dimension(280, 42));
            return p;
        }

        void showStart() {
            card.removeAll();
            add(lbl("<span style='color:#c084fc'>Swift</span>le", 30, TEXT, true), 6);
            add(lbl("Guess the Taylor Swift song in " + MAX_GUESSES + " tries", 13, MUTED, false), 18);
            add(mode("Easy  ·  standard album tracks", EXACT, true), 8);
            add(mode("Swiftie  ·  all " + SONGS.size() + " songs", BTN, false), 14);
            add(lbl("Swiftie mode includes vault, deluxe,<br>soundtrack, holiday & collab songs", 11, MUTED, false), 0);
            open();
        }

        void showEnd() {
            card.removeAll();
            add(lbl(won ? (guesses.size() == 1 ? "FIRST TRY?! ICONIC" : "You got it, Swiftie!") : "So close!", 22, ACCENT, true), 12);
            add(new AnswerCard(answer), 14);
            String pct = played == 0 ? "0" : String.valueOf(Math.round(100.0 * wins / played));
            add(lbl(played + " played  ·  " + pct + "% won  ·  streak " + streak + "  ·  best " + best, 12, MUTED, false), 16);
            add(mode("Next song  ·  same mode", BTN, easyMode), 8);
            add(mode(easyMode ? "Switch to Swiftie mode" : "Switch to Easy mode", MISS, !easyMode), 0);
            open();
        }

        void open() {
            card.revalidate();
            setVisible(true);
            revalidate();
            repaint();
            requestFocusInWindow();
        }
    }

    static class AnswerCard extends JComponent {
        final Song s;
        AnswerCard(Song s) {
            this.s = s;
            int h = s.title.length() > 26 ? 104 : 84;
            setPreferredSize(new Dimension(300, h));
            setMaximumSize(new Dimension(300, h));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color bg = ERA.get(s.era), fg = textOn(bg);
            g.setColor(bg);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(fg);
            g.setFont(new Font(FONT, Font.BOLD, 17));
            List<String> t = wrap(s.title, g.getFontMetrics(), getWidth() - 28, 2);
            int y = 30;
            for (String l : t) { g.drawString(l, 14, y); y += 20; }
            g.setFont(new Font(FONT, Font.PLAIN, 12));
            String sub = s.album + "  ·  " + s.year + (s.track > 0 ? "  ·  track " + s.track : "") + "  ·  " + s.length();
            List<String> subl = wrap(sub, g.getFontMetrics(), getWidth() - 28, 1);
            g.drawString(subl.get(0), 14, getHeight() - (s.feat.isEmpty() ? 16 : 32));
            if (!s.feat.isEmpty()) {
                List<String> fl = wrap("feat. " + s.feat, g.getFontMetrics(), getWidth() - 28, 1);
                g.drawString(fl.get(0), 14, getHeight() - 14);
            }
            g.dispose();
        }
    }

    static void purpleScroll(JScrollPane sp) {
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar = sp.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = new Color(0x5b3a8f); trackColor = new Color(0x1a0b2e); }
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }
            JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(thumbColor);
                g.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
                g.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height); }
        });
    }

    public void hubStart() { field.requestFocusInWindow(); }
    public void hubResume() { field.requestFocusInWindow(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new swiftle().setVisible(true));
    }
}