package net.veldor.rutrackertv.selections;

import java.io.Serializable;

@SuppressWarnings("NullableProblems")
public class Distribution  implements Serializable {
    public String Category;
    public String Size;
    public String Seeds;
    public String Name;
    public String Href;
    public String TorrentHref;
    public boolean IsMovie;
    public ExtendedDistributionInfo ExtendedInfo;
    public String ContentType;

    @Override
    public String toString() {
        return "Distribution{" +
                "Category=" + Category +
                ", Size='" + Size + '\'' +
                ", Seeds='" + Seeds + '\'' +
                ", Name='" + Name + '\'' +
                ", Href='" + Href + '\'' +
                ", TorrentHref='" + TorrentHref + '\'' +
                ", ExtendedInfo='" + ExtendedInfo + '\'' +
                '}';
    }
}
