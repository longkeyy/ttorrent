import com.turn.ttorrent.cli.ClientMain;
import com.turn.ttorrent.cli.TorrentMain;
import com.turn.ttorrent.cli.TrackerMain;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.listen.Listenable;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

/**
 * Created by Administrator on 15-6-10.
 */
public class TestClient {
    public static void main(String[] args) {

        TrackerMain.main(args);
        args = new String[]{"-t","ai.torrent","-c","-a","http://192.168.54.34:6969/announce","d:/tmp/torrent/master/ai"};
        TorrentMain.main(args);
        args = new String[]{"-o","d:/tmp/torrent/master","ai.torrent"};
        ClientMain.main(args);
        args = new String[]{"-o","d:/tmp/torrent/slave","ai.torrent"};
        ClientMain.main(args);


    }


}
