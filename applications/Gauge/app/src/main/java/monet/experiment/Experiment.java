package monet.experiment;

import com.happen.it.make.whatisit.MxNetGauge;

/**
 * Created by tianlins on 1/20/16.
 */
public interface Experiment {
    void run();

    MxNetGauge getGauge();
}
