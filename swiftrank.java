import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class swiftrank extends JFrame {

    static final Color BG = new Color(0x1a0b2e);
    static final Color PANEL = new Color(0x2a1650);
    static final Color BORDER = new Color(0x4c1d95);
    static final Color TEXT = new Color(0xf3e8ff);
    static final Color MUTED = new Color(0xa78bca);
    static final Color ACCENT = new Color(0xc084fc);
    static final Color BTN = new Color(0x7c3aed);
    static final Color DIM = new Color(0x2c2140);
    static final String FONT = "SansSerif";

    static final String[][] ERAS = {
            {"debut", "Taylor Swift", "1D4737"}, {"fearless", "Fearless", "CBA863"},
            {"speaknow", "Speak Now", "72325F"}, {"red", "Red", "A91E47"},
            {"1989", "1989", "8BB5D2"}, {"reputation", "reputation", "2C2C2C"},
            {"lover", "Lover", "EBBED3"}, {"folklore", "folklore", "949494"},
            {"evermore", "evermore", "6B3324"}, {"midnights", "Midnights", "5A658B"},
            {"ttpd", "TTPD", "ADA795"}, {"showgirl", "Showgirl", "C44615"},
            {"holiday", "Holiday", "7A1F2B"}, {"singles", "Singles & OST", "5B3A8F"},
            {"collabs", "Collabs", "6CAE90"}};
    static final Map<String, Color> ERA_COLOR = new HashMap<>();
    static final Map<String, String> ERA_NAME = new HashMap<>();
    static {
        for (String[] e : ERAS) { ERA_COLOR.put(e[0], new Color(Integer.parseInt(e[2], 16))); ERA_NAME.put(e[0], e[1]); }
    }

    static Color textOn(Color bg) {
        double l = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255;
        return l > 0.55 ? new Color(0x1a0b2e) : Color.WHITE;
    }

    static Color lift(Color c, int d) {
        return new Color(Math.max(0, Math.min(255, c.getRed() + d)), Math.max(0, Math.min(255, c.getGreen() + d)),
                Math.max(0, Math.min(255, c.getBlue() + d)));
    }

    static class Song {
        final String title, album, era, feat, type;
        final int track, sec, year;
        Song(String line) {
            String[] p = line.split("\\|", -1);
            title = p[0]; album = p[1]; era = p[2];
            track = Integer.parseInt(p[3]); sec = Integer.parseInt(p[4]);
            feat = p[5]; year = Integer.parseInt(p[6]); type = p[7];
        }
        String length() { return sec / 60 + ":" + String.format("%02d", sec % 60); }
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

    static class NeedChoice extends RuntimeException {
        final int a, b; final String label;
        NeedChoice(int a, int b, String label) { super(null, null, false, false); this.a = a; this.b = b; this.label = label; }
    }

    static class Session {
        String mode;
        String scope;
        List<Integer> order = new ArrayList<>();
        StringBuilder answers = new StringBuilder();
        int pos;
        String label = "";

        boolean pick(int a, int b) {
            if (pos < answers.length()) return answers.charAt(pos++) == 'L';
            throw new NeedChoice(a, b, label);
        }

        List<Integer> merge() {
            pos = 0;
            List<List<Integer>> runs = new ArrayList<>();
            for (int s : order) runs.add(new ArrayList<>(List.of(s)));
            while (runs.size() > 1) {
                List<List<Integer>> next = new ArrayList<>();
                for (int i = 0; i < runs.size(); i += 2) {
                    if (i + 1 == runs.size()) { next.add(runs.get(i)); continue; }
                    List<Integer> x = runs.get(i), y = runs.get(i + 1), out = new ArrayList<>();
                    int p = 0, q = 0;
                    while (p < x.size() && q < y.size()) {
                        if (pick(x.get(p), y.get(q))) out.add(x.get(p++)); else out.add(y.get(q++));
                    }
                    while (p < x.size()) out.add(x.get(p++));
                    while (q < y.size()) out.add(y.get(q++));
                    next.add(out);
                }
                runs = next;
            }
            return runs.isEmpty() ? new ArrayList<>() : runs.get(0);
        }

        List<List<Integer>> bracket() {
            pos = 0;
            int n = order.size();
            int p = Integer.highestOneBit(n);
            if (p < n) p <<= 1;
            List<List<Integer>> tiers = new ArrayList<>();
            List<Integer> alive = new ArrayList<>(order);
            int round = 0;
            while (alive.size() > 1) {
                List<Integer> next = new ArrayList<>(), out = new ArrayList<>();
                int size = alive.size();
                int matches, start;
                if (round == 0 && p != n) { matches = n - p / 2; start = n - 2 * matches; next.addAll(alive.subList(0, start)); }
                else { matches = size / 2; start = 0; }
                String rname = size == 2 ? "Final" : size == 4 ? "Semifinals" : size == 8 ? "Quarterfinals"
                        : "Round of " + (round == 0 ? p : size);
                for (int m = 0; m < matches; m++) {
                    int a = alive.get(start + 2 * m), b = alive.get(start + 2 * m + 1);
                    label = rname + "  ·  match " + (m + 1) + " of " + matches;
                    if (pick(a, b)) { next.add(a); out.add(b); } else { next.add(b); out.add(a); }
                }
                tiers.add(0, out);
                alive = next;
                round++;
            }
            tiers.add(0, alive);
            return tiers;
        }

        int estimate() {
            int n = order.size();
            if (mode.equals("bracket")) return Math.max(0, n - 1);
            List<Integer> runs = new ArrayList<>(Collections.nCopies(n, 1));
            int total = 0;
            while (runs.size() > 1) {
                List<Integer> next = new ArrayList<>();
                for (int i = 0; i < runs.size(); i += 2) {
                    if (i + 1 == runs.size()) { next.add(runs.get(i)); continue; }
                    total += runs.get(i) + runs.get(i + 1) - 1;
                    next.add(runs.get(i) + runs.get(i + 1));
                }
                runs = next;
            }
            return total;
        }

        String encode() {
            StringBuilder sb = new StringBuilder();
            for (int i : order) { if (sb.length() > 0) sb.append(','); sb.append(i); }
            return mode + ";" + scope + ";" + SONGS.size() + ";" + sb + ";" + answers;
        }

        static Session decode(String s) {
            try {
                String[] p = s.split(";", -1);
                if (Integer.parseInt(p[2]) != SONGS.size()) return null;
                Session x = new Session();
                x.mode = p[0]; x.scope = p[1];
                for (String i : p[3].split(",")) x.order.add(Integer.parseInt(i));
                x.answers.append(p[4]);
                return x;
            } catch (Exception e) { return null; }
        }
    }

    static final int W = 600, H = 560, RH = 40;
    final Preferences prefs = tryPrefs();
    final Random rng = new Random();
    final CardLayout cards = new CardLayout();
    final JPanel root = new JPanel(cards);
    final Duel duel = new Duel();
    final JPanel home = new JPanel(), fullSetup = new JPanel(), bracketSetup = new JPanel(), results = new JPanel();
    Session session;
    NeedChoice current;

    public swiftrank() {
        super("Swift Rank");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        root.setBackground(BG);
        root.setPreferredSize(new Dimension(W, H));
        for (JPanel p : new JPanel[]{home, fullSetup, bracketSetup, results}) {
            p.setBackground(BG);
            p.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        }
        root.add(home, "home");
        root.add(fullSetup, "full");
        root.add(bracketSetup, "bracket");
        root.add(duel, "duel");
        root.add(results, "results");
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || !duel.isShowing() || !isActive()) return false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT: choose('L'); return true;
                case KeyEvent.VK_RIGHT: choose('R'); return true;
                case KeyEvent.VK_SPACE: choose(rng.nextBoolean() ? 'L' : 'R'); return true;
                case KeyEvent.VK_BACK_SPACE: undo(); return true;
            }
            return false;
        });
        showHome();
    }

    static Preferences tryPrefs() {
        try { return Preferences.userRoot().node("swiftrank-diva"); } catch (Exception e) { return null; }
    }

    void save() {
        if (prefs == null || session == null) return;
        try { prefs.put("session", session.encode()); } catch (Exception ignored) {}
    }

    Session loadSaved() {
        if (prefs == null) return null;
        String s = prefs.get("session", null);
        return s == null ? null : Session.decode(s);
    }

    void clearSaved() {
        if (prefs != null) try { prefs.remove("session"); } catch (Exception ignored) {}
    }

    static JLabel label(String html, int size, Color c, boolean bold) {
        JLabel l = new JLabel("<html>" + html + "</html>");
        l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }

    static class Pill extends JComponent {
        String text, sub; Color base; boolean hover, on = true; final Runnable action;
        Pill(String text, String sub, Color base, int w, int h, Runnable action) {
            this.text = text; this.sub = sub; this.base = base; this.action = action;
            setPreferredSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
            setMinimumSize(new Dimension(w, h));
            setAlignmentX(LEFT_ALIGNMENT);
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
            Color c = on ? base : DIM;
            if (hover) c = lift(c, 22);
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            Color fg = on ? textOn(base) : MUTED;
            g.setColor(fg);
            if (sub == null) {
                g.setFont(new Font(FONT, Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            } else {
                g.setFont(new Font(FONT, Font.BOLD, 17));
                g.drawString(text, 18, 30);
                g.setFont(new Font(FONT, Font.PLAIN, 12));
                g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 200));
                int y = 50;
                for (String l : sub.split("\n")) { g.drawString(l, 18, y); y += 16; }
            }
            g.dispose();
        }
    }

    void showHome() {
        home.removeAll();
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));
        home.add(label("<span style='color:#c084fc'>Swift</span> Rank", 30, TEXT, true));
        home.add(Box.createVerticalStrut(4));
        home.add(label("Rank Taylor Swift songs by picking your favorite, one pair at a time.", 13, MUTED, false));
        home.add(Box.createVerticalStrut(22));

        Session saved = loadSaved();
        if (saved != null) {
            String what = saved.mode.equals("full") ? "Full ranking" : "Bracket · " + scopeName(saved.scope);
            home.add(new Pill("Continue: " + what, saved.answers.length() + " picks made so far  ·  " + saved.order.size() + " songs",
                    BTN, W - 44, 72, () -> start(saved)));
            home.add(Box.createVerticalStrut(12));
        }
        home.add(new Pill("Full ranking", "Rank every song 1 to N. Choose which eras to include;\nall " + SONGS.size()
                + " songs takes about 2,000 picks (saved as you go).", new Color(0x5b3a8f), W - 44, 80, this::showFullSetup));
        home.add(Box.createVerticalStrut(12));
        home.add(new Pill("Album bracket", "Knockout tournament for one album (or every song).\nCrowns a champion and shows the final four, top 8...",
                new Color(0x7a2d8f), W - 44, 80, this::showBracketSetup));
        home.add(Box.createVerticalGlue());
        home.add(label("Song data: “taylor” dataset by W. Jake Thompson + Wikipedia", 10, new Color(0x6f5a90), false));
        home.revalidate(); home.repaint();
        cards.show(root, "home");
    }

    static String scopeName(String scope) {
        return scope.equals("all") ? "All songs" : ERA_NAME.getOrDefault(scope, scope);
    }

    static int count(Set<String> eras) {
        int n = 0;
        for (Song s : SONGS) if (eras.contains(s.era)) n++;
        return n;
    }

    final Set<String> chosen = new LinkedHashSet<>();

    void showFullSetup() {
        if (chosen.isEmpty()) for (String[] e : ERAS) chosen.add(e[0]);
        fullSetup.removeAll();
        fullSetup.setLayout(new BoxLayout(fullSetup, BoxLayout.Y_AXIS));
        fullSetup.add(label("Full ranking", 24, TEXT, true));
        fullSetup.add(Box.createVerticalStrut(4));
        fullSetup.add(label("Pick the eras to include:", 13, MUTED, false));
        fullSetup.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(5, 3, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(W - 44, 5 * 44 + 32));
        JLabel info = label("", 13, TEXT, true);
        Runnable upd = () -> {
            Session tmp = new Session();
            tmp.mode = "full";
            for (int i = 0; i < count(chosen); i++) tmp.order.add(i);
            info.setText("<html>" + count(chosen) + " songs  ·  up to " + tmp.estimate() + " picks</html>");
        };
        for (String[] e : ERAS) {
            int n = count(Set.of(e[0]));
            Pill[] ref = new Pill[1];
            ref[0] = new Pill(e[1] + " (" + n + ")", null, ERA_COLOR.get(e[0]), 170, 44, () -> {
                if (!chosen.remove(e[0])) chosen.add(e[0]);
                ref[0].on = chosen.contains(e[0]);
                ref[0].repaint();
                upd.run();
            });
            ref[0].on = chosen.contains(e[0]);
            grid.add(ref[0]);
        }
        fullSetup.add(grid);
        fullSetup.add(Box.createVerticalStrut(14));
        upd.run();
        fullSetup.add(info);
        fullSetup.add(Box.createVerticalGlue());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(new Pill("Back", null, DIM, 100, 42, this::showHome));
        row.add(Box.createHorizontalStrut(10));
        row.add(new Pill("All", null, DIM, 70, 42, () -> { for (String[] e : ERAS) chosen.add(e[0]); showFullSetup(); }));
        row.add(Box.createHorizontalStrut(10));
        row.add(new Pill("None", null, DIM, 70, 42, () -> { chosen.clear(); chosen.add("__none__"); showFullSetup(); chosen.remove("__none__"); }));
        row.add(Box.createHorizontalStrut(10));
        row.add(new Pill("Start ranking", null, BTN, 180, 42, () -> {
            if (count(chosen) < 2) { info.setText("<html><span style='color:#ec4899'>Pick at least 2 songs, diva!</span></html>"); return; }
            Session s = new Session();
            s.mode = "full";
            s.scope = String.join(",", chosen);
            for (int i = 0; i < SONGS.size(); i++) if (chosen.contains(SONGS.get(i).era)) s.order.add(i);
            Collections.shuffle(s.order, rng);
            start(s);
        }));
        fullSetup.add(row);
        fullSetup.revalidate(); fullSetup.repaint();
        cards.show(root, "full");
    }

    void showBracketSetup() {
        bracketSetup.removeAll();
        bracketSetup.setLayout(new BoxLayout(bracketSetup, BoxLayout.Y_AXIS));
        bracketSetup.add(label("Album bracket", 24, TEXT, true));
        bracketSetup.add(Box.createVerticalStrut(4));
        bracketSetup.add(label("Pick an album to start its tournament:", 13, MUTED, false));
        bracketSetup.add(Box.createVerticalStrut(12));
        JPanel grid = new JPanel(new GridLayout(6, 3, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(W - 44, 6 * 44 + 40));
        for (String[] e : ERAS) {
            int n = count(Set.of(e[0]));
            grid.add(new Pill(e[1] + " (" + n + ")", null, ERA_COLOR.get(e[0]), 170, 44, () -> startBracket(e[0])));
        }
        grid.add(new Pill("All songs (" + SONGS.size() + ")", null, BTN, 170, 44, () -> startBracket("all")));
        bracketSetup.add(grid);
        bracketSetup.add(Box.createVerticalGlue());
        bracketSetup.add(new Pill("Back", null, DIM, 100, 42, this::showHome));
        bracketSetup.revalidate(); bracketSetup.repaint();
        cards.show(root, "bracket");
    }

    void startBracket(String era) {
        Session s = new Session();
        s.mode = "bracket";
        s.scope = era;
        for (int i = 0; i < SONGS.size(); i++) if (era.equals("all") || SONGS.get(i).era.equals(era)) s.order.add(i);
        Collections.shuffle(s.order, rng);
        start(s);
    }

    void start(Session s) {
        session = s;
        save();
        advance();
    }

    void advance() {
        try {
            if (session.mode.equals("full")) {
                session.label = "";
                List<Integer> ranked = session.merge();
                showResults(ranked, null);
            } else {
                List<List<Integer>> tiers = session.bracket();
                showResults(null, tiers);
            }
            clearSaved();
        } catch (NeedChoice c) {
            current = c;
            duel.update();
            cards.show(root, "duel");
        }
    }

    void choose(char side) {
        if (current == null) return;
        session.answers.append(side);
        duel.flash(side);
        save();
        advance();
    }

    void undo() {
        if (session == null || session.answers.length() == 0) return;
        session.answers.setLength(session.answers.length() - 1);
        save();
        advance();
    }

    class Duel extends JPanel {
        final JLabel head = label("", 14, ACCENT, true);
        final JLabel progress = label("", 12, MUTED, false);
        final SongCard left = new SongCard('L'), right = new SongCard('R');
        final JProgressBar bar = new JProgressBar(0, 1000);

        Duel() {
            setBackground(BG);
            setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            setLayout(new BorderLayout(0, 14));
            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(label("Which one do you prefer?", 22, TEXT, true));
            top.add(Box.createVerticalStrut(4));
            top.add(head);
            add(top, BorderLayout.NORTH);

            JPanel mid = new JPanel(new GridLayout(1, 2, 16, 0));
            mid.setOpaque(false);
            mid.add(left);
            mid.add(right);
            add(mid, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
            bar.setForeground(BTN);
            bar.setBackground(DIM);
            bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(W - 44, 8));
            bar.setMaximumSize(new Dimension(W - 44, 8));
            bar.setAlignmentX(LEFT_ALIGNMENT);
            bottom.add(bar);
            bottom.add(Box.createVerticalStrut(6));
            bottom.add(progress);
            bottom.add(Box.createVerticalStrut(12));
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.add(new Pill("Undo", null, DIM, 90, 38, swiftrank.this::undo));
            row.add(Box.createHorizontalStrut(8));
            row.add(new Pill("Can't decide", null, DIM, 130, 38, () -> choose(rng.nextBoolean() ? 'L' : 'R')));
            row.add(Box.createHorizontalStrut(8));
            row.add(new Pill("Save & exit", null, DIM, 130, 38, () -> { save(); showHome(); }));
            bottom.add(row);
            add(bottom, BorderLayout.SOUTH);
        }

        void update() {
            int done = session.answers.length(), est = Math.max(1, session.estimate());
            boolean full = session.mode.equals("full");
            head.setText("<html>" + (full ? "Full ranking  ·  " + session.order.size() + " songs" :
                    esc(scopeName(session.scope)) + " bracket  ·  " + esc(current.label)) + "</html>");
            int pct = Math.min(99, (int) Math.round(100.0 * done / est));
            bar.setValue(Math.min(999, done * 1000 / est));
            progress.setText("<html>" + done + " picks made  ·  " + (full ? "about " + pct + "% done" : (est - done) + " matches left")
                    + "  ·  ← → to pick, Backspace to undo</html>");
            left.song = SONGS.get(current.a);
            right.song = SONGS.get(current.b);
            left.repaint(); right.repaint();
        }

        void flash(char side) { (side == 'L' ? left : right).pulse = System.currentTimeMillis(); }
    }

    class SongCard extends JComponent {
        Song song; boolean hover; long pulse; final char side;
        SongCard(char side) {
            this.side = side;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseReleased(MouseEvent e) { if (contains(e.getPoint())) choose(side); }
            });
        }
        @Override protected void paintComponent(Graphics g0) {
            if (song == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color bg = ERA_COLOR.get(song.era), fg = textOn(bg);
            int lift = hover ? 6 : 0;
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(4, 10, getWidth() - 8, getHeight() - 12, 22, 22);
            g.setColor(hover ? lift(bg, 14) : bg);
            g.fillRoundRect(0, 6 - lift, getWidth() - 4, getHeight() - 12, 22, 22);
            if (hover) {
                g.setColor(ACCENT);
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(1, 7 - lift, getWidth() - 6, getHeight() - 14, 22, 22);
            }
            int x = 20, w = getWidth() - 44;
            g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 170));
            g.setFont(new Font(FONT, Font.BOLD, 11));
            g.drawString((side == 'L' ? "←  " : "→  ") + ERA_NAME.get(song.era).toUpperCase(), x, 34 - lift);

            g.setColor(fg);
            g.setFont(new Font(FONT, Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = wrap(song.title, fm, w, 4);
            if (lines.size() > 3) { g.setFont(new Font(FONT, Font.BOLD, 19)); fm = g.getFontMetrics(); lines = wrap(song.title, fm, w, 4); }
            int y = 78 - lift;
            for (String l : lines) { g.drawString(l, x, y); y += fm.getHeight(); }

            g.setFont(new Font(FONT, Font.PLAIN, 13));
            fm = g.getFontMetrics();
            List<String> info = new ArrayList<>();
            info.add(song.album + "  ·  " + song.year);
            info.add((song.track > 0 ? "Track " + song.track + "  ·  " : "") + song.length()
                    + (song.type.equals("Standard") ? "" : "  ·  " + song.type));
            if (!song.feat.isEmpty()) info.add((song.era.equals("collabs") ? "with " : "feat. ") + song.feat);
            int iy = getHeight() - 22 - lift - (info.size() - 1) * 18;
            for (String l : info) {
                g.drawString(wrap(l, fm, w, 1).get(0), x, iy);
                iy += 18;
            }
            g.dispose();
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
        if (out.isEmpty()) out.add("");
        return out;
    }

    void showResults(List<Integer> ranked, List<List<Integer>> tiers) {
        current = null;
        List<String[]> rows = new ArrayList<>();
        StringBuilder txt = new StringBuilder();
        String title;
        if (ranked != null) {
            title = "Your Taylor Swift ranking";
            txt.append("My Taylor Swift song ranking (").append(ranked.size()).append(" songs)\n\n");
            for (int i = 0; i < ranked.size(); i++) {
                rows.add(new String[]{String.valueOf(i + 1), String.valueOf(ranked.get(i))});
                Song s = SONGS.get(ranked.get(i));
                txt.append(i + 1).append(". ").append(s.title).append(" — ").append(s.album).append('\n');
            }
        } else {
            title = scopeName(session.scope) + " bracket results";
            txt.append("My ").append(scopeName(session.scope)).append(" bracket\n\n");
            int place = 1;
            for (int t = 0; t < tiers.size(); t++) {
                String tier = t == 0 ? "Winner" : t == 1 ? "Runner-up" : t == 2 ? "Top 4" : "Top " + (1 << t);
                txt.append(tier).append(":\n");
                for (int idx : tiers.get(t)) {
                    rows.add(new String[]{t <= 1 ? String.valueOf(place) : tier, String.valueOf(idx)});
                    Song s = SONGS.get(idx);
                    txt.append("  ").append(s.title).append(" — ").append(s.album).append('\n');
                    place++;
                }
            }
        }

        results.removeAll();
        results.setLayout(new BorderLayout(0, 12));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(label(esc(title), 22, TEXT, true));
        top.add(Box.createVerticalStrut(4));
        top.add(label("Number 1 is your favorite. SLAY!", 12, MUTED, false));
        results.add(top, BorderLayout.NORTH);

        RankList list = new RankList(rows);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        purpleScroll(sp);
        results.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel msg = label(" ", 11, MUTED, false);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(new Pill("Copy list", null, BTN, 120, 40, () -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txt.toString()), null);
            msg.setText("<html>Copied! Paste it anywhere ♥</html>");
        }));
        row.add(Box.createHorizontalStrut(8));
        row.add(new Pill("Save as .txt", null, BTN, 130, 40, () -> {
            File f = new File(System.getProperty("user.home"),
                    session.mode.equals("full") ? "taylor-swift-ranking.txt" : "taylor-swift-bracket.txt");
            try {
                Files.write(f.toPath(), txt.toString().getBytes(StandardCharsets.UTF_8));
                msg.setText("<html>Saved to " + esc(f.getAbsolutePath()) + "</html>");
            } catch (Exception ex) { msg.setText("<html>Couldn't save: " + esc(String.valueOf(ex.getMessage())) + "</html>"); }
        }));
        row.add(Box.createHorizontalStrut(8));
        row.add(new Pill("Undo last pick", null, DIM, 140, 40, swiftrank.this::undo));
        row.add(Box.createHorizontalStrut(8));
        row.add(new Pill("Home", null, DIM, 90, 40, this::showHome));
        bottom.add(row);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(msg);
        results.add(bottom, BorderLayout.SOUTH);
        results.revalidate(); results.repaint();
        cards.show(root, "results");
    }

    class RankList extends JComponent {
        final List<String[]> rows;
        RankList(List<String[]> rows) { this.rows = rows; }
        @Override public Dimension getPreferredSize() { return new Dimension(W - 64, rows.size() * RH + 4); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Rectangle clip = g.getClipBounds();
            int first = Math.max(0, clip.y / RH - 1), last = Math.min(rows.size(), (clip.y + clip.height) / RH + 2);
            for (int i = first; i < last; i++) {
                String[] r = rows.get(i);
                Song s = SONGS.get(Integer.parseInt(r[1]));
                int y = i * RH;
                boolean top3 = i < 3;
                Color era = ERA_COLOR.get(s.era);
                g.setColor(top3 ? era : PANEL);
                g.fillRoundRect(0, y + 2, getWidth() - 2, RH - 4, 12, 12);
                if (!top3) {
                    g.setColor(era);
                    g.fillRoundRect(0, y + 2, 7, RH - 4, 6, 6);
                }
                Color fg = top3 ? textOn(era) : TEXT;
                g.setColor(top3 ? fg : ACCENT);
                g.setFont(new Font(FONT, Font.BOLD, r[0].length() > 3 ? 11 : 15));
                g.drawString(i == 0 ? "♛ 1" : r[0], 16, y + 25);
                g.setColor(fg);
                g.setFont(new Font(FONT, Font.BOLD, 14));
                FontMetrics fm = g.getFontMetrics();
                int tx = 70;
                String alb = s.album;
                g.drawString(wrap(s.title, fm, getWidth() - tx - 150, 1).get(0), tx, y + 25);
                g.setFont(new Font(FONT, Font.PLAIN, 11));
                fm = g.getFontMetrics();
                g.setColor(top3 ? fg : MUTED);
                alb = wrap(alb, fm, 130, 1).get(0);
                g.drawString(alb, getWidth() - 14 - fm.stringWidth(alb), y + 25);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new swiftrank().setVisible(true));
    }
}