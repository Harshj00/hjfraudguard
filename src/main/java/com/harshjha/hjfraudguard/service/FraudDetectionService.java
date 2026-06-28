package com.harshjha.hjfraudguard.service;

import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.inference.Predictor;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FraudDetectionService {

    // From your Colab training output
    private static final double SCALER_MEAN = 88.34961925093133;
    private static final double SCALER_STD = 250.11967013523534;
    private static final double THRESHOLD = 6.532095;

    private Predictor<float[], float[]> predictor;

    @PostConstruct
    public void init() throws Exception {
        // DJL needs a real filesystem path, so copy the model out of the jar/classpath into a temp folder
        Path modelDir = Files.createTempDirectory("djl-model");
        ClassPathResource resource = new ClassPathResource("models/fraud_autoencoder.pt");
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, modelDir.resolve("fraud_autoencoder.pt"));
        }

        Model model = Model.newInstance("fraud_autoencoder", "PyTorch");
        model.load(modelDir, "fraud_autoencoder");

        Translator<float[], float[]> translator = new Translator<float[], float[]>() {
            @Override
            public NDList processInput(TranslatorContext ctx, float[] input) {
                NDManager manager = ctx.getNDManager();
                NDArray array = manager.create(input, new Shape(1, input.length));
                return new NDList(array);
            }

            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }
        };

        predictor = model.newPredictor(translator);
    }

    public double computeAnomalyScore(double amount) throws Exception {
        float scaledAmount = (float) ((amount - SCALER_MEAN) / SCALER_STD);

        // IMPORTANT: training column order was V1...V28, Amount LAST (not first)
        float[] input = new float[29];
        for (int i = 0; i < 28; i++) {
            input[i] = 0f; // V1-V28 default to their standardized mean (demo-mode simplification)
        }
        input[28] = scaledAmount;

        float[] reconstructed = predictor.predict(input);

        double sumSquaredError = 0;
        for (int i = 0; i < input.length; i++) {
            double diff = input[i] - reconstructed[i];
            sumSquaredError += diff * diff;
        }
        return sumSquaredError / input.length;
    }

    public boolean isFraud(double anomalyScore) {
        return anomalyScore > THRESHOLD;
    }
}