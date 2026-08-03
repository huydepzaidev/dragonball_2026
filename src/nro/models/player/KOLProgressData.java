package nro.models.player;

/**
 *
 * @author By Mr Blue
 */
public class KOLProgressData {

    public static final int CURRENT_VERSION = 2;

    public int version;
    public int kolQuestStage;
    public long woodDummyAutoTrainKills;
    public long birdDemonAutoTrainKills;
    public long fideWaveCompletions;
    public long challengeWins;
    public long hardDailyQuestCompletions;
    public long superBrolyDefeats;

    public KOLProgressData() {
        this.version = 0;
        this.kolQuestStage = 1;
    }
}
