package it.units.inginf.male.objective;

import it.units.inginf.male.utils.BasicStats;
import it.units.inginf.male.evaluators.TreeEvaluationException;
import it.units.inginf.male.evaluators.TreeEvaluator;
import it.units.inginf.male.inputs.Context;
import it.units.inginf.male.inputs.DataSet;
import it.units.inginf.male.inputs.DataSet.Bounds;
import it.units.inginf.male.inputs.DataSet.Example;
import it.units.inginf.male.tree.Node;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MultiObjective fitness, char fpr + fnr, ABS(Number_Extractions -
 * Number_Matches)), and regex length: three fitnesses, three objectives {FPR +
 * FNR, ABS(Number_Extractions - Number_Matches)), regexLength}
 *
 * @author nvhuy9527
 */
public class CharmaskMatchLengthObjective implements Objective {

    private Context context;
    
    @Override
    public void setup(Context context) {
        this.context = context;
        
    }

    @Override
    public double[] fitness(Node individual) {
        DataSet dataSetView = this.context.getCurrentDataSet();
        TreeEvaluator evaluator = context.getConfiguration().getEvaluator();
        double[] fitness = new double[3];

        double fitnessLenght;

        List<List<Bounds>> evaluate;
        try {
            evaluate = evaluator.evaluate(individual, context);
            StringBuilder builder = new StringBuilder();
            individual.describe(builder);
            fitnessLenght = builder.length();
        } catch (TreeEvaluationException ex) {
            Logger.getLogger(CharmaskMatchLengthObjective.class.getName()).log(Level.SEVERE, null, ex);
            Arrays.fill(fitness, Double.POSITIVE_INFINITY);
            return fitness;
        }

        //Calculates the true positive and false positive matches:
        
        //match stats makes sense only for tp e fp values... we cannot use instance statistic formulas other than precision
        BasicStats statsOverall = new BasicStats();

        //char stats can be managed as ususal
        BasicStats statsCharsOverall = new BasicStats();

        int i = 0;
        for (List<Bounds> result : evaluate) {
            BasicStats stats = new BasicStats();
            BasicStats statsChars = new BasicStats();
            //Characted extracted in the right place (match)
            Example example = dataSetView.getExample(i);
            List<Bounds> expectedMatchMask = example.getMatch();
            List<Bounds> expectedUnmatchMask = example.getUnmatch();
            List<Bounds> annotatedMask = new ArrayList<>(expectedMatchMask);
            annotatedMask.addAll(expectedUnmatchMask);

            stats.tp = countIdenticalRanges(result, expectedMatchMask);
            stats.fp = Bounds.countRangesThatCollideZone(result, annotatedMask) - stats.tp;
            statsChars.tp = intersection(result, expectedMatchMask);
            statsChars.fp = intersection(result, expectedUnmatchMask);

            statsOverall.add(stats);
            statsCharsOverall.add(statsChars);
            i++;
        }

        statsCharsOverall.tn = dataSetView.getNumberUnmatchedChars() - statsCharsOverall.fp;
        statsCharsOverall.fn = dataSetView.getNumberMatchedChars() - statsCharsOverall.tp;

        fitness[0] = (statsCharsOverall.fpr() + statsCharsOverall.fnr()) * 100.0;
        fitness[1] = Math.abs(statsOverall.fp + statsOverall.tp - dataSetView.getNumberMatches()); // ABS((TP+FP) - (TP+FN)) alias ABS(Number_Extractions - Number_Matches))
        fitness[2] = fitnessLenght;

        return fitness;
    }

    //number of chars of this extracted rages which falls into expected ranges
    private int intersection(List<Bounds> extractedRanges, List<Bounds> expectedRanges) {
        int overallNumChars = 0;
         
        for (Bounds extractedBounds : extractedRanges) {
            for (Bounds expectedBounds : expectedRanges) {
                int numChars = Math.min(extractedBounds.end, expectedBounds.end) - Math.max(extractedBounds.start, expectedBounds.start);
                overallNumChars += Math.max(0, numChars);
            }
        }
        return overallNumChars;
    }

    //number of idential intervals
    private int countIdenticalRanges(List<Bounds> rangesA, List<Bounds> rangesB) {
        int identicalRanges = 0;
         
        for (Bounds boundsA : rangesA) {
            for (Bounds boundsB : rangesB) {
                if (boundsA.equals(boundsB)) {
                    identicalRanges++;
                    break;
                }
            }
        }
        return identicalRanges;
    }

    @Override
    public TreeEvaluator getTreeEvaluator() {
        return context.getConfiguration().getEvaluator();
    }

    @Override
    public Objective cloneObjective() {
        return new CharmaskMatchLengthObjective();
    }
}
