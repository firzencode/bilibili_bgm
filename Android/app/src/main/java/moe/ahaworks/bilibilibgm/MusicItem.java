package moe.ahaworks.bilibilibgm;

/**
 * Created by firzencode on 15/7/30.
 */
public class MusicItem {
    public String mTitle;
    public long mAvid;
    public long mTime;
    public int mPardId;
    public String mPartTitle;
    public String mFilePath;

    public MusicItem(String title, long avid, long time, int partId, String partTitle, String filePath) {
        mTitle = title;
        mAvid = avid;
        mTime = time;
        mPardId = partId;
        mPartTitle = partTitle;
        mFilePath = filePath;
    }
}