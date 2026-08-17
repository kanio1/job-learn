package lab.paymentquality.testing.internal.web;

import lab.paymentquality.testing.internal.seed.DataLearningTruth;

record LearningSeedResponse(String operation, String status, DataLearningTruth truth) {

    static LearningSeedResponse completed(DataLearningTruth truth) {
        return new LearningSeedResponse("seed-learning", "completed", truth);
    }
}
