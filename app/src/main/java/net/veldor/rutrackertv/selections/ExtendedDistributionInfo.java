package net.veldor.rutrackertv.selections;

import java.io.Serializable;

public class ExtendedDistributionInfo  implements Serializable {


    public static final String ADDITIONAL_DATA = "additional data";

    public String PostImageUrl;
    public String PostBody;
    public String PageHref;
    public String FileName;

    @Override
    public String toString() {
        return "Distribution{" +
                "PostImageUrl=" + PostImageUrl +
                ", PostBody='" + PostBody + '\'' +
                ", PageHref='" + PageHref + '\'' +
                ", FileName='" + FileName + '\'' +
                '}';
    }
}
