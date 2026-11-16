#include "BoweiJni.h"
#include "math.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "android/log.h"

static const char *TAG = "BoweiJni";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)


//double xTest[] = {35.13,35.13,35.18,35.21,35.21,35.24,35.26,35.29,35.34,35.34,35.37,35.37,35.45,35.45,35.5,35.56,35.53,36.57,37.03,37.16,37.29,37.29,37.37,37.48,37.45,37.45,37.51,37.59,37.61,37.64,37.64,37.67,37.72,37.8,37.77,37.83,37.96,37.8,37.91,37.96,37.96,37.96,38.04,38.07,38.18,38.15,38.15,38.18,38.2,38.26,38.28,38.28,38.39,38.39,38.39,38.47,38.5,38.47,38.52,38.66,38.58,38.63,38.69,38.74,38.69,38.74,38.74,38.79,38.79,38.77,38.79,38.82,38.87,38.87,38.9,38.98,38.98,38.95,39.11,39.17,39.14,39.17,39.22,39.3,39.33,39.41,39.41,39.52,39.54,39.54,39.54,39.62,39.68,39.65,39.73,39.81,39.78,39.89,39.92,39.97,40.08,40.05,40.13,40.13,40.19,40.32,40.32,40.32,40.4,40.48,40.51,40.54,40.59,40.67,40.72,40.72,40.83,40.91,40.91,40.97,40.99,41.02,41.13,41.15,41.21,41.23,41.29,41.37,41.42,41.45,41.45,41.53,41.61,41.64,41.66,41.75,41.77,41.85,41.91,41.91,41.99,42.07,42.07,42.18,42.23,42.23,42.26,42.39,42.39,42.39,42.39,42.5,42.55,42.66,42.66,42.71,42.8,42.85,42.88,42.88,42.96,43.06,43.09,43.15,43.2,43.28,43.39,43.31,43.39,43.41,43.52,43.5,43.58,43.63,43.71,43.76,43.79,43.87,43.87,43.9,44.01,44.03,44.14,44.22,44.2,44.25,44.3,44.33,44.41,44.55,44.55,44.49,44.63,44.66,44.74,44.74,44.79,44.84,44.87,44.95,45.03,45.03,45.11,45.17,45.22,45.25,45.28,45.36,45.41,45.47,45.47,45.63,45.6,45.63,45.76,45.79,45.76,45.9,45.92,45.92,45.98,46.03,46.09,46.19,46.25,46.22,46.3,46.36,46.38,46.41,46.55,46.52,46.63,46.65,46.65,46.82,46.82,46.87,46.87,46.95,47.03,47.03,47.14,47.14,47.2,47.22,47.33,47.36,47.41,47.52,47.44,47.58,47.71,47.6,47.68,47.74,47.79,47.9,47.87,47.95,47.98,48.09,48.09,48.09,48.23,48.25,48.31,48.33,48.42,48.42,48.5,48.55,48.63,48.69,48.69,48.77,48.79,48.85,48.9,48.96,49.01,49.09,49.07,49.15,49.2,49.26,49.26,49.36,49.42,49.47,49.47,49.55,49.61,49.69,49.77,49.74,49.83,49.88,49.88,49.96,49.99,50.12,50.1,50.18,50.21,50.26,50.31,50.34,50.37,50.45,50.56,50.59,50.59,50.64,50.7,50.78,50.8,50.8,50.94,50.94,50.99,51.08,51.13,51.13,51.27,51.27,51.29,51.35,51.43,51.46,51.48,51.57,51.67,51.7,51.65,51.76,51.78,51.87,51.87,51.97,52.0,52.0,52.14,52.16,52.19,52.27,52.33,52.36,52.38,52.44,52.55,52.6,52.6,52.66,52.66,52.79,52.82,52.82,52.9,52.95,53.01,53.06,53.09,53.2,53.23,53.28,53.34,53.36,53.47,53.5,53.53,53.58,53.64,53.69,53.72,53.75,53.75,53.85,53.94,53.94,53.99,54.07,54.07,54.18,54.26,54.21,54.29,54.4,54.4,54.46,54.56,54.62,54.62,54.67,54.73,54.73,54.89,54.87,54.92,54.92,55.0,55.03,55.14,55.14,55.19,55.3,55.36,55.36,55.38,55.44,55.52,55.55,55.63,55.66,55.71,55.77,55.77,55.82,55.88,56.01,55.96,56.07,56.1,56.31,56.12,56.26,56.31,56.45,56.42,56.53,56.48,56.59,56.59,56.64,56.7,56.78,56.86,56.86,56.92,57.0,57.05,57.05,57.11,57.11,57.3,57.27,57.27,57.35,57.41,57.46,57.49,57.55,57.57,57.68,57.74,57.77,57.85,57.88,57.9,57.99,57.99,58.12,58.12,58.18,58.2,58.26,58.31,58.34,58.42,58.48,58.51,58.56,58.56,58.62,58.7,58.75,58.75,58.89,58.92,58.92,59.06,59.06,59.14,59.17,59.19,59.25,59.27,59.38,59.38,59.52,59.52,59.52,59.66,59.63,59.74,59.8,59.85,59.91,59.93,59.96,60.02,60.04,60.1,60.13,60.26,60.24,60.29,60.43,60.4,60.4,60.48,60.57,60.59,60.68,60.7,60.79,60.76,60.87,61.01,61.03,61.01,61.12,61.12,61.2,61.31,61.23,61.31,61.31,61.42,61.45,61.47,61.56,61.64,61.61,61.69,61.75,61.78,61.89,61.92,61.94,62.03,62.05,62.11,62.22,62.3,62.22,62.27,62.38,62.38,62.44,62.52,62.63,62.55,62.6,62.66,62.77,62.82,62.91,62.91,62.96,62.96,63.02,63.07,63.16,63.18,63.32,63.38,63.38,63.43,63.43,63.49,63.54,63.62,63.68,63.71,63.79,63.9,63.87,63.9,64.04,64.01,64.07,64.12,64.15,64.15,64.26,64.31,64.34,64.45,64.45,64.51,64.54,64.59,64.56,64.7,64.81,64.87,64.84,64.92,64.98,65.01,65.09,65.14,65.14,65.2,65.28,65.31,65.37,65.42,65.45,65.48,65.59,65.64,65.72,65.78,65.78,65.86,65.89,65.95,66.0,66.0,66.03,66.17,66.22,66.22,66.28,66.28,66.44,66.44,66.53,66.61,66.56,66.58,66.67,66.75,66.8,66.86,66.83,66.94,67.03,67.05,67.14,67.14,67.19,67.22,67.28,67.33,67.36,67.44,67.44,67.58,67.53,67.69,67.64,67.66,67.77,67.83,67.89,67.91,67.97,68.02,68.08,68.05,68.22,68.25,68.3,68.36,68.41,68.41,68.5,68.55,68.61,68.63,68.69,68.66,68.77,68.83,68.86,68.86,68.94,69.05,69.11,69.13,69.22,69.19,69.27,69.3,69.3,69.52,69.47,69.52,69.58,69.58,69.66,69.72,69.77,69.8,69.88,69.94,69.88,70.05,70.08,70.13,70.16,70.22,70.25,70.33,70.39,70.41,70.44,70.47,70.61,70.66,70.69,70.69,70.75,70.8,70.89,71.0,70.97,71.0,71.14,71.11,71.19,71.16,71.28,71.3,71.47,71.39,71.47,71.53,71.61,71.67,71.72,71.69,71.72,71.8,71.89,71.94,72.03,72.0,72.03,72.11,72.17,72.22,72.25,72.28,72.39,72.47,72.5,72.5,72.56,72.56,72.64,72.72,72.78,72.84,72.95,72.95,73.03,73.0,73.06,73.14,73.14,73.25,73.28,73.34,73.31,73.42,73.42,73.51,73.56,73.53,73.65,73.73,73.76,73.81,73.84,73.9,73.98,74.04,74.12,74.15,74.2,74.15,74.26,74.26,74.37,74.46,74.46,74.51,74.57,74.65,74.65,74.71,74.74,74.85,74.87,74.9,74.96,75.04,75.04,75.13,75.18,75.24,75.32,75.32,75.41,75.41,75.49,75.52,75.55,75.63,75.69,75.66,75.77,75.83,75.88,75.88,75.94,76.02,76.08,76.08,76.22,76.22,76.25,76.33,76.36,76.39,76.53,76.5,76.47,76.67,76.67,76.69,76.81,76.81,76.81,76.95,76.95,77.06,77.09,77.14,77.14,77.23,77.31,77.28,77.37,77.48,77.51,77.53,77.59,77.59,77.62,77.67,77.79,77.84,77.87,77.87,77.98,78.04,78.01,78.1,78.18,78.18,78.29,78.26,78.32,78.4,78.43,78.52,78.54,78.57,78.68,78.77,78.77,78.8,78.85,78.94,78.99,78.99,79.11,79.11,79.16,79.22,79.3,79.33,79.42,79.5,79.47,79.53,79.61,79.58,79.67,79.72,79.78,79.81,79.89,79.98,79.98,79.95,80.09,80.15,80.15,80.2,80.31,80.29,80.4,80.4,80.54,80.54,80.6,80.6,80.71,80.74,80.74,80.82,80.88,80.91,80.93,81.02,81.02,81.13,81.22,81.19,81.27,81.3,81.33,81.5,81.47,81.55,81.55,81.58,81.69,81.78,81.75,81.83,81.81,82.0,82.0,82.03,82.06,82.14,82.14,82.2,82.26,82.31,82.4,82.4,82.48,82.57,82.54,82.6,82.74,82.71,82.79,82.85,82.88,82.93,82.93,82.99,83.1,83.13,83.19,83.19,83.3,83.36,83.36,83.41,83.47,83.53,83.61,83.58,83.64,83.72,83.81,83.87,83.84,83.87,83.92,84.06,84.06,84.18,84.15,84.23,84.23,84.26,84.35,84.37,84.43,84.49,84.63,84.63,84.66,84.71,84.74,84.77,84.91,84.91,85.0,85.0};
//double testDataTest[] = {6492.0,6497.0,6502.0,6506.0,6511.0,6516.0,6520.0,6524.0,6529.0,6534.0,6539.0,6546.0,6553.0,6562.0,6572.0,6584.0,6598.0,6613.0,6630.0,6648.0,6666.0,6685.0,6705.0,6723.0,6742.0,6759.0,6775.0,6790.0,6804.0,6816.0,6827.0,6838.0,6847.0,6856.0,6865.0,6873.0,6881.0,6888.0,6896.0,6903.0,6910.0,6918.0,6924.0,6931.0,6938.0,6944.0,6950.0,6956.0,6961.0,6967.0,6972.0,6977.0,6982.0,6987.0,6992.0,6998.0,7003.0,7009.0,7016.0,7022.0,7029.0,7036.0,7043.0,7050.0,7057.0,7063.0,7070.0,7076.0,7082.0,7088.0,7094.0,7099.0,7105.0,7110.0,7115.0,7120.0,7125.0,7129.0,7134.0,7138.0,7143.0,7147.0,7152.0,7157.0,7162.0,7168.0,7174.0,7180.0,7187.0,7194.0,7202.0,7209.0,7218.0,7226.0,7234.0,7243.0,7251.0,7259.0,7268.0,7276.0,7284.0,7292.0,7300.0,7308.0,7317.0,7325.0,7333.0,7341.0,7349.0,7356.0,7364.0,7372.0,7379.0,7387.0,7395.0,7402.0,7410.0,7418.0,7426.0,7435.0,7443.0,7451.0,7460.0,7468.0,7477.0,7486.0,7495.0,7504.0,7514.0,7524.0,7534.0,7544.0,7555.0,7565.0,7575.0,7585.0,7594.0,7604.0,7613.0,7621.0,7630.0,7639.0,7647.0,7656.0,7665.0,7674.0,7683.0,7693.0,7702.0,7713.0,7723.0,7733.0,7743.0,7753.0,7762.0,7771.0,7780.0,7788.0,7796.0,7804.0,7811.0,7819.0,7826.0,7834.0,7842.0,7850.0,7858.0,7867.0,7876.0,7885.0,7895.0,7905.0,7915.0,7925.0,7936.0,7946.0,7957.0,7967.0,7978.0,7988.0,7998.0,8009.0,8018.0,8028.0,8037.0,8047.0,8056.0,8065.0,8074.0,8083.0,8093.0,8102.0,8112.0,8121.0,8131.0,8141.0,8151.0,8161.0,8171.0,8181.0,8191.0,8200.0,8209.0,8218.0,8227.0,8235.0,8243.0,8251.0,8258.0,8266.0,8273.0,8281.0,8288.0,8296.0,8303.0,8311.0,8319.0,8327.0,8335.0,8343.0,8351.0,8359.0,8367.0,8375.0,8383.0,8391.0,8400.0,8408.0,8417.0,8425.0,8434.0,8443.0,8451.0,8460.0,8468.0,8476.0,8483.0,8491.0,8498.0,8504.0,8511.0,8517.0,8523.0,8529.0,8536.0,8542.0,8548.0,8554.0,8561.0,8568.0,8574.0,8581.0,8589.0,8596.0,8604.0,8611.0,8619.0,8627.0,8635.0,8643.0,8652.0,8660.0,8668.0,8676.0,8684.0,8691.0,8699.0,8706.0,8713.0,8720.0,8727.0,8734.0,8740.0,8747.0,8754.0,8760.0,8767.0,8773.0,8780.0,8787.0,8794.0,8801.0,8808.0,8815.0,8822.0,8830.0,8837.0,8844.0,8851.0,8858.0,8864.0,8871.0,8877.0,8883.0,8888.0,8894.0,8899.0,8905.0,8910.0,8916.0,8921.0,8927.0,8933.0,8939.0,8945.0,8951.0,8957.0,8963.0,8968.0,8974.0,8979.0,8984.0,8989.0,8994.0,8999.0,9003.0,9008.0,9013.0,9019.0,9024.0,9030.0,9037.0,9043.0,9050.0,9056.0,9063.0,9070.0,9077.0,9084.0,9091.0,9098.0,9106.0,9113.0,9120.0,9127.0,9135.0,9142.0,9148.0,9154.0,9160.0,9165.0,9170.0,9174.0,9177.0,9179.0,9181.0,9183.0,9184.0,9185.0,9187.0,9188.0,9190.0,9192.0,9195.0,9199.0,9203.0,9207.0,9213.0,9218.0,9224.0,9230.0,9236.0,9242.0,9249.0,9255.0,9261.0,9266.0,9272.0,9277.0,9283.0,9288.0,9293.0,9297.0,9302.0,9306.0,9311.0,9315.0,9319.0,9323.0,9326.0,9330.0,9333.0,9337.0,9340.0,9344.0,9347.0,9351.0,9354.0,9358.0,9362.0,9365.0,9369.0,9373.0,9377.0,9380.0,9384.0,9387.0,9391.0,9394.0,9398.0,9401.0,9404.0,9408.0,9410.0,9413.0,9415.0,9416.0,9417.0,9417.0,9416.0,9416.0,9415.0,9414.0,9414.0,9414.0,9414.0,9415.0,9417.0,9420.0,9423.0,9426.0,9431.0,9435.0,9440.0,9446.0,9451.0,9456.0,9460.0,9465.0,9469.0,9472.0,9475.0,9478.0,9480.0,9481.0,9482.0,9483.0,9483.0,9482.0,9481.0,9480.0,9479.0,9477.0,9476.0,9474.0,9473.0,9473.0,9472.0,9473.0,9473.0,9474.0,9475.0,9475.0,9476.0,9476.0,9476.0,9475.0,9473.0,9471.0,9468.0,9465.0,9462.0,9458.0,9453.0,9449.0,9445.0,9440.0,9436.0,9431.0,9427.0,9422.0,9418.0,9413.0,9409.0,9405.0,9400.0,9396.0,9392.0,9388.0,9385.0,9381.0,9378.0,9375.0,9372.0,9368.0,9365.0,9362.0,9358.0,9353.0,9348.0,9343.0,9336.0,9330.0,9323.0,9315.0,9308.0,9300.0,9293.0,9286.0,9279.0,9272.0,9265.0,9259.0,9252.0,9245.0,9238.0,9230.0,9223.0,9215.0,9207.0,9198.0,9190.0,9181.0,9172.0,9164.0,9155.0,9147.0,9139.0,9131.0,9123.0,9116.0,9109.0,9101.0,9094.0,9087.0,9079.0,9072.0,9064.0,9056.0,9048.0,9040.0,9031.0,9024.0,9016.0,9009.0,9002.0,8996.0,8990.0,8985.0,8979.0,8974.0,8969.0,8963.0,8957.0,8952.0,8946.0,8940.0,8935.0,8930.0,8925.0,8921.0,8918.0,8915.0,8912.0,8910.0,8908.0,8906.0,8905.0,8903.0,8902.0,8900.0,8899.0,8898.0,8897.0,8896.0,8896.0,8895.0,8895.0,8895.0,8896.0,8897.0,8899.0,8901.0,8904.0,8908.0,8911.0,8915.0,8919.0,8922.0,8924.0,8926.0,8926.0,8925.0,8923.0,8920.0,8917.0,8913.0,8910.0,8907.0,8904.0,8902.0,8901.0,8901.0,8901.0,8903.0,8904.0,8907.0,8909.0,8912.0,8915.0,8918.0,8921.0,8924.0,8926.0,8929.0,8931.0,8933.0,8934.0,8935.0,8936.0,8937.0,8938.0,8939.0,8939.0,8940.0,8940.0,8941.0,8942.0,8943.0,8944.0,8946.0,8948.0,8950.0,8953.0,8957.0,8960.0,8964.0,8967.0,8970.0,8974.0,8976.0,8979.0,8981.0,8983.0,8984.0,8985.0,8986.0,8987.0,8988.0,8989.0,8990.0,8991.0,8992.0,8993.0,8995.0,8996.0,8997.0,8998.0,8999.0,9000.0,9001.0,9003.0,9004.0,9005.0,9007.0,9008.0,9010.0,9011.0,9013.0,9014.0,9016.0,9018.0,9020.0,9022.0,9024.0,9027.0,9029.0,9032.0,9034.0,9036.0,9039.0,9041.0,9043.0,9045.0,9047.0,9048.0,9050.0,9052.0,9053.0,9055.0,9056.0,9058.0,9059.0,9059.0,9060.0,9061.0,9061.0,9061.0,9062.0,9062.0,9063.0,9064.0,9065.0,9067.0,9068.0,9070.0,9072.0,9074.0,9076.0,9077.0,9079.0,9079.0,9080.0,9080.0,9080.0,9081.0,9081.0,9081.0,9081.0,9081.0,9082.0,9082.0,9083.0,9083.0,9084.0,9084.0,9084.0,9084.0,9085.0,9085.0,9085.0,9085.0,9085.0,9085.0,9085.0,9086.0,9086.0,9087.0,9088.0,9089.0,9090.0,9090.0,9091.0,9092.0,9092.0,9092.0,9092.0,9092.0,9091.0,9089.0,9088.0,9086.0,9084.0,9082.0,9080.0,9079.0,9077.0,9076.0,9076.0,9076.0,9077.0,9078.0,9079.0,9080.0,9082.0,9083.0,9085.0,9087.0,9088.0,9089.0,9090.0,9090.0,9091.0,9091.0,9091.0,9091.0,9092.0,9092.0,9092.0,9093.0,9094.0,9095.0,9096.0,9096.0,9097.0,9098.0,9099.0,9099.0,9099.0,9100.0,9100.0,9101.0,9102.0,9103.0,9104.0,9105.0,9106.0,9107.0,9108.0,9109.0,9109.0,9110.0,9110.0,9110.0,9109.0,9109.0,9107.0,9106.0,9104.0,9101.0,9099.0,9096.0,9094.0,9092.0,9090.0,9088.0,9086.0,9085.0,9084.0,9084.0,9083.0,9083.0,9084.0,9084.0,9085.0,9086.0,9086.0,9087.0,9088.0,9089.0,9089.0,9090.0,9090.0,9091.0,9091.0,9092.0,9092.0,9093.0,9094.0,9095.0,9096.0,9097.0,9098.0,9100.0,9101.0,9102.0,9103.0,9104.0,9104.0,9105.0,9105.0,9105.0,9104.0,9104.0,9103.0,9102.0,9101.0,9100.0,9100.0,9099.0,9099.0,9098.0,9098.0,9098.0,9098.0,9098.0,9098.0,9099.0,9099.0,9099.0,9100.0,9100.0,9101.0,9101.0,9102.0,9102.0,9102.0,9101.0,9101.0,9100.0,9098.0,9097.0,9095.0,9094.0,9092.0,9091.0,9091.0,9090.0,9090.0,9090.0,9090.0,9090.0,9090.0,9090.0,9090.0,9090.0,9090.0,9089.0,9089.0,9089.0,9089.0,9089.0,9088.0,9088.0,9088.0,9087.0,9087.0,9087.0,9086.0,9086.0,9085.0,9085.0,9084.0,9083.0,9082.0,9080.0,9079.0,9077.0,9074.0,9072.0,9070.0,9067.0,9065.0,9063.0,9061.0,9060.0,9059.0,9058.0,9058.0,9057.0,9058.0,9058.0,9058.0,9058.0,9058.0,9058.0,9059.0,9058.0,9058.0,9058.0,9057.0,9057.0,9056.0,9055.0,9053.0,9052.0,9050.0,9048.0,9047.0,9045.0,9043.0,9041.0,9040.0,9039.0,9038.0,9037.0,9036.0,9036.0,9036.0,9035.0,9035.0,9034.0,9033.0,9032.0,9031.0,9029.0,9028.0,9026.0,9024.0,9021.0,9019.0,9017.0,9015.0,9013.0,9012.0,9010.0,9009.0,9009.0,9009.0,9009.0,9010.0,9011.0,9012.0,9013.0,9014.0,9015.0,9016.0,9017.0,9017.0,9018.0,9017.0,9017.0,9016.0,9015.0,9014.0,9012.0,9010.0,9008.0,9006.0,9004.0,9002.0,9000.0,8998.0,8996.0,8994.0,8993.0,8991.0,8989.0,8987.0,8985.0,8984.0};



// 判断对象是否是某个Java类的实例
jboolean isInstanceOf(JNIEnv *env, jobject obj, jclass clazz) {
    jclass objClass = env->GetObjectClass(obj);
    jboolean isInstance = env->IsInstanceOf(obj, clazz);
    env->DeleteLocalRef(objClass);
    return isInstance;
}


extern "C" JNIEXPORT void Java_com_winters_boweilibrary_BoweiJni_analysisExperimentLoessFilter
        (JNIEnv* env, jobject cls, jobject experiment ){
    //est();
    //jobject experiment = env->NewGlobalRef(obj);
    jclass clz = env->GetObjectClass(experiment);
    jfieldID fidName = env->GetFieldID(clz, "Name", "Ljava/lang/String;");
    //jstring newValue = env->NewStringUTF("新的字符创");
    //env->SetObjectField(experiment, fidName, newValue);
    //const char a[] = "ssssss";
    //LOGI("The init is %d" , 1212121);
    jfieldID wellResultFieldID = env->GetFieldID(clz, "WellResult", "Ljava/util/List;");

    ////熔解曲线步长
    double interval = 0.1;

    /////Loess滤波步长
    int stepPreLoess = 100;
    int stepPostLoess = 100;


    jfieldID TypestepPreLoess = env->GetFieldID(clz, "stepPreLoess", "I");
    jfieldID TypestepPostLoess = env->GetFieldID(clz, "stepPostLoess", "I");
    stepPreLoess = env->GetIntField(experiment, TypestepPreLoess);
    stepPostLoess = env->GetIntField(experiment, TypestepPostLoess);

    //////优化算法参数
    bool optimization_enabl_FAM = true;
    double enhance_lower_FAM = 2;
    double enhance_upper_FAM = 5;
    double enhance_gain_FAM = 1.5;
    double weaken_FAM = 2;
    double weaken_gain_FAM = 0.5;
    double slope_thres_FAM = 0.3;

    jfieldID Typeoptimization_enabl_FAM = env->GetFieldID(clz, "optimization_enabl_FAM", "Z");
    jfieldID Typeenhance_lower_FAM = env->GetFieldID(clz, "enhance_lower_FAM", "D");
    jfieldID Typeenhance_upper_FAM = env->GetFieldID(clz, "enhance_upper_FAM", "D");
    jfieldID Typeenhance_gain_FAM = env->GetFieldID(clz, "enhance_gain_FAM", "D");
    jfieldID Typeweaken_FAM = env->GetFieldID(clz, "weaken_FAM", "D");
    jfieldID Typeweaken_gain_FAM = env->GetFieldID(clz, "weaken_gain_FAM", "D");
    jfieldID Typeslope_thres_FAM = env->GetFieldID(clz, "slope_thres_FAM", "D");

    optimization_enabl_FAM = env->GetBooleanField(experiment, Typeoptimization_enabl_FAM);
    enhance_lower_FAM = env->GetDoubleField(experiment, Typeenhance_lower_FAM);
    enhance_upper_FAM = env->GetDoubleField(experiment, Typeenhance_upper_FAM);
    enhance_gain_FAM = env->GetDoubleField(experiment, Typeenhance_gain_FAM);
    weaken_FAM = env->GetDoubleField(experiment, Typeweaken_FAM);
    weaken_gain_FAM = env->GetDoubleField(experiment, Typeweaken_gain_FAM);
    slope_thres_FAM = env->GetDoubleField(experiment, Typeslope_thres_FAM);

    LOGE("optimization_enabl_FAM %d\n",optimization_enabl_FAM);
    LOGE("enhance_lower_FAM %f\n",enhance_lower_FAM);
    LOGE("enhance_upper_FAM %f\n",enhance_upper_FAM);
    LOGE("enhance_gain_FAM %f\n",enhance_gain_FAM);
    LOGE("weaken_FAM %f\n",weaken_FAM);
    LOGE("weaken_gain_FAM %f\n",weaken_gain_FAM);
    LOGE("slope_thres_FAM %f\n",slope_thres_FAM);



    bool optimization_enabl_HEX = true;
    double enhance_lower_HEX = 2;
    double enhance_upper_HEX = 5;
    double enhance_gain_HEX = 1.5;
    double weaken_HEX = 2;
    double weaken_gain_HEX = 0.5;
    double slope_thres_HEX = 0.3;

    jfieldID Typeoptimization_enabl_HEX = env->GetFieldID(clz, "optimization_enabl_HEX", "Z");
    jfieldID Typeenhance_lower_HEX = env->GetFieldID(clz, "enhance_lower_HEX", "D");
    jfieldID Typeenhance_upper_HEX = env->GetFieldID(clz, "enhance_upper_HEX", "D");
    jfieldID Typeenhance_gain_HEX = env->GetFieldID(clz, "enhance_gain_HEX", "D");
    jfieldID Typeweaken_HEX = env->GetFieldID(clz, "weaken_HEX", "D");
    jfieldID Typeweaken_gain_HEX = env->GetFieldID(clz, "weaken_gain_HEX", "D");
    jfieldID Typeslope_thres_HEX = env->GetFieldID(clz, "slope_thres_HEX", "D");

    optimization_enabl_HEX = env->GetBooleanField(experiment, Typeoptimization_enabl_HEX);
    enhance_lower_HEX = env->GetDoubleField(experiment, Typeenhance_lower_HEX);
    enhance_upper_HEX = env->GetDoubleField(experiment, Typeenhance_upper_HEX);
    enhance_gain_HEX = env->GetDoubleField(experiment, Typeenhance_gain_HEX);
    weaken_HEX = env->GetDoubleField(experiment, Typeweaken_HEX);
    weaken_gain_HEX = env->GetDoubleField(experiment, Typeweaken_gain_HEX);
    slope_thres_HEX = env->GetDoubleField(experiment, Typeslope_thres_HEX);

    LOGE("optimization_enabl_HEX %d\n",optimization_enabl_HEX);
    LOGE("enhance_lower_HEX %f\n",enhance_lower_HEX);
    LOGE("enhance_upper_HEX %f\n",enhance_upper_HEX);
    LOGE("enhance_gain_HEX %f\n",enhance_gain_HEX);
    LOGE("weaken_HEX %f\n",weaken_HEX);
    LOGE("weaken_gain_HEX %f\n",weaken_gain_HEX);
    LOGE("slope_thres_HEX %f\n",slope_thres_HEX);

    bool optimization_enabl_ROX = true;
    double enhance_lower_ROX = 2;
    double enhance_upper_ROX = 5;
    double enhance_gain_ROX = 1.5;
    double weaken_ROX = 2;
    double weaken_gain_ROX = 0.5;
    double slope_thres_ROX = 0.3;

    jfieldID Typeoptimization_enabl_ROX = env->GetFieldID(clz, "optimization_enabl_ROX", "Z");
    jfieldID Typeenhance_lower_ROX = env->GetFieldID(clz, "enhance_lower_ROX", "D");
    jfieldID Typeenhance_upper_ROX = env->GetFieldID(clz, "enhance_upper_ROX", "D");
    jfieldID Typeenhance_gain_ROX = env->GetFieldID(clz, "enhance_gain_ROX", "D");
    jfieldID Typeweaken_ROX = env->GetFieldID(clz, "weaken_ROX", "D");
    jfieldID Typeweaken_gain_ROX = env->GetFieldID(clz, "weaken_gain_ROX", "D");
    jfieldID Typeslope_thres_ROX = env->GetFieldID(clz, "slope_thres_ROX", "D");

    optimization_enabl_ROX = env->GetBooleanField(experiment, Typeoptimization_enabl_ROX);
    enhance_lower_ROX = env->GetDoubleField(experiment, Typeenhance_lower_ROX);
    enhance_upper_ROX = env->GetDoubleField(experiment, Typeenhance_upper_ROX);
    enhance_gain_ROX = env->GetDoubleField(experiment, Typeenhance_gain_ROX);
    weaken_ROX = env->GetDoubleField(experiment, Typeweaken_ROX);
    weaken_gain_ROX = env->GetDoubleField(experiment, Typeweaken_gain_ROX);
    slope_thres_ROX = env->GetDoubleField(experiment, Typeslope_thres_ROX);

    LOGE("optimization_enabl_ROX %d\n",optimization_enabl_ROX);
    LOGE("enhance_lower_ROX %f\n",enhance_lower_ROX);
    LOGE("enhance_upper_ROX %f\n",enhance_upper_ROX);
    LOGE("enhance_gain_ROX %f\n",enhance_gain_ROX);
    LOGE("weaken_ROX %f\n",weaken_ROX);
    LOGE("weaken_gain_ROX %f\n",weaken_gain_ROX);
    LOGE("slope_thres_ROX %f\n",slope_thres_ROX);


    bool optimization_enabl_CY5 = true;
    double enhance_lower_CY5 = 2;
    double enhance_upper_CY5 = 5;
    double enhance_gain_CY5 = 1.5;
    double weaken_CY5 = 2;
    double weaken_gain_CY5 = 0.5;
    double slope_thres_CY5 = 0.3;

    jfieldID Typeoptimization_enabl_CY5 = env->GetFieldID(clz, "optimization_enabl_CY5", "Z");
    jfieldID Typeenhance_lower_CY5 = env->GetFieldID(clz, "enhance_lower_CY5", "D");
    jfieldID Typeenhance_upper_CY5 = env->GetFieldID(clz, "enhance_upper_CY5", "D");
    jfieldID Typeenhance_gain_CY5 = env->GetFieldID(clz, "enhance_gain_CY5", "D");
    jfieldID Typeweaken_CY5 = env->GetFieldID(clz, "weaken_CY5", "D");
    jfieldID Typeweaken_gain_CY5 = env->GetFieldID(clz, "weaken_gain_CY5", "D");
    jfieldID Typeslope_thres_CY5 = env->GetFieldID(clz, "slope_thres_CY5", "D");

    optimization_enabl_CY5 = env->GetBooleanField(experiment, Typeoptimization_enabl_CY5);
    enhance_lower_CY5 = env->GetDoubleField(experiment, Typeenhance_lower_CY5);
    enhance_upper_CY5 = env->GetDoubleField(experiment, Typeenhance_upper_CY5);
    enhance_gain_CY5 = env->GetDoubleField(experiment, Typeenhance_gain_CY5);
    weaken_CY5 = env->GetDoubleField(experiment, Typeweaken_CY5);
    weaken_gain_CY5 = env->GetDoubleField(experiment, Typeweaken_gain_CY5);
    slope_thres_CY5 = env->GetDoubleField(experiment, Typeslope_thres_CY5);

    LOGE("optimization_enabl_CY5 %d\n",optimization_enabl_CY5);
    LOGE("enhance_lower_CY5 %f\n",enhance_lower_CY5);
    LOGE("enhance_upper_CY5 %f\n",enhance_upper_CY5);
    LOGE("enhance_gain_CY5 %f\n",enhance_gain_CY5);
    LOGE("weaken_CY5 %f\n",weaken_CY5);
    LOGE("weaken_gain_CY5 %f\n",weaken_gain_CY5);
    LOGE("slope_thres_CY5 %f\n",slope_thres_CY5);

    ///////获取实验类型，
    jfieldID projectID = env->GetFieldID(clz, "Project",
                                         "Lcom/hongshi/slansdk/model/experiment/Project;");
    jobject project = env->GetObjectField(experiment, projectID);

    jclass projectclz = env->GetObjectClass(project);

    jfieldID TypeID = env->GetFieldID(projectclz, "Type", "I");

    jfieldID AMPAlgorithmID = env->GetFieldID(projectclz, "AMPAlgorithm", "I");

    int type = env->GetIntField(project, TypeID);// 0	定性/定量     1	标准熔解曲线

    int AMPAlgorithm = env->GetIntField(project, AMPAlgorithmID); //0	相对荧光值法log   1	绝对荧光值法  2	相对荧光值法 - 线性

    LOGE("The experiment type is %d \n" , type);

    LOGE("The experiment AMPAlgorithm is %d \n" , AMPAlgorithm);

    bool capture_FAM = false;//	自动阀值
    bool capture_HEX = false;//	自动阀值
    bool capture_ROX = false;//	自动阀值
    bool capture_CY5 = false;//	自动阀值


    //////////////////获取扩增及熔解的分析参数
    int BeginBaseline_FAM = 6;//基线起点
    int EndBaseline_FAM = 12;//基线终点
    bool AutoThreshold_FAM = false;//	自动阀值
    double Threshold_FAM = 0.12;//非自动阈值时，手动阀值设置值
    int OptimizationMode_FAM = 1 ; //0	不优化  1	自动优化  2	手动优化
    int DigitalFilter_FAM = 2; //0	不滤波    1	轻度滤波    2	重度滤波
    /////////////////////////melt
    double MinTemperature_FAM;//最小温度
    double MaxTemperature_FAM;//最大温度
    double PeekWidthThreshold_FAM;//峰宽阈值
    double PeekHightThreshold_FAM;//峰高阈值
    int PeekType_FAM; //0	正峰  1	负峰  2	正负峰
    int MeltPeakAlgorithm_FAM;//0	标准算法（以最大Y值为Tm）  1	高级算法

    int BeginBaseline_HEX = 6;//基线起点
    int EndBaseline_HEX = 12;//基线终点
    bool AutoThreshold_HEX = false;//	自动阀值
    double Threshold_HEX = 0.12;//非自动阈值时，手动阀值设置值
    int OptimizationMode_HEX = 1; //0	不优化  1	自动优化  2	手动优化
    int DigitalFilter_HEX = 2; //0	不滤波    1	轻度滤波    2	重度滤波
    /////////////////////////melt
    double MinTemperature_HEX;//最小温度
    double MaxTemperature_HEX;//最大温度
    double PeekWidthThreshold_HEX;//峰宽阈值
    double PeekHightThreshold_HEX;//峰高阈值
    int PeekType_HEX; //0	正峰  1	负峰  2	正负峰
    int MeltPeakAlgorithm_HEX;//0	标准算法（以最大Y值为Tm）  1	高级算法

    int BeginBaseline_ROX = 6;//基线起点
    int EndBaseline_ROX = 12;//基线终点
    bool AutoThreshold_ROX = false;//	自动阀值
    double Threshold_ROX = 0.12;//非自动阈值时，手动阀值设置值
    int OptimizationMode_ROX = 1; //0	不优化  1	自动优化  2	手动优化
    int DigitalFilter_ROX = 2; //0	不滤波    1	轻度滤波    2	重度滤波
    /////////////////////////melt
    double MinTemperature_ROX;//最小温度
    double MaxTemperature_ROX;//最大温度
    double PeekWidthThreshold_ROX;//峰宽阈值
    double PeekHightThreshold_ROX;//峰高阈值
    int PeekType_ROX; //0	正峰  1	负峰  2	正负峰
    int MeltPeakAlgorithm_ROX;//0	标准算法（以最大Y值为Tm）  1	高级算法

    int BeginBaseline_CY5 = 6;//基线起点
    int EndBaseline_CY5 = 12;//基线终点
    bool AutoThreshold_CY5 = false;//	自动阀值
    double Threshold_CY5 = 0.12;//非自动阈值时，手动阀值设置值
    int OptimizationMode_CY5 = 1; //0	不优化  1	自动优化  2	手动优化
    int DigitalFilter_CY5 = 2; //0	不滤波    1	轻度滤波    2	重度滤波
    /////////////////////////melt
    double MinTemperature_CY5;//最小温度
    double MaxTemperature_CY5;//最大温度
    double PeekWidthThreshold_CY5;//峰宽阈值
    double PeekHightThreshold_CY5;//峰高阈值
    int PeekType_CY5; //0	正峰  1	负峰  2	正负峰
    int MeltPeakAlgorithm_CY5;//0	标准算法（以最大Y值为Tm）  1	高级算法


    jfieldID ProgramID = env->GetFieldID(projectclz, "Program",
                                         "Lcom/hongshi/slansdk/model/experiment/Program;");

    jobject valueProgram = env->GetObjectField(project, ProgramID);

    jclass valueProgramclz = env->GetObjectClass(valueProgram);

    jfieldID SectionsID = env->GetFieldID(valueProgramclz, "Sections", "Ljava/util/ArrayList;");

    jobject valueSections = env->GetObjectField(valueProgram, SectionsID);

    jclass ArrayListvalueSectionsclz = env->GetObjectClass(valueSections);

    jmethodID ArrayListsizeMethodvalueSectionsID = env->GetMethodID(ArrayListvalueSectionsclz, "size", "()I");

    jmethodID ArrayListgetMethodvalueSectionsID = env->GetMethodID(ArrayListvalueSectionsclz, "get", "(I)Ljava/lang/Object;");

    jsize SectionsSize = env->CallIntMethod(valueSections, ArrayListsizeMethodvalueSectionsID);

    for(int i = 0; i < SectionsSize;i++){

        jobject valueSection= env->CallObjectMethod(valueSections, ArrayListgetMethodvalueSectionsID,i);

        jclass Sectionclz = env->GetObjectClass(valueSection);

        // 获取类对象
        jclass clazzSectionMelt = env->FindClass("com/hongshi/slansdk/model/experiment/SectionMelt");
        // 判断并返回结果
        jboolean result = isInstanceOf(env, valueSection, clazzSectionMelt);
        if(result){
            jfieldID IntervalID = env->GetFieldID(Sectionclz, "Interval","D");
            interval = env->GetDoubleField(valueSection, IntervalID);
            //LOGE("Interval %f\n",interval);
            break;
        }
        env->DeleteLocalRef(clazzSectionMelt);
        env->DeleteLocalRef(Sectionclz);

    }

    jfieldID TargetsFieldID = env->GetFieldID(projectclz, "Targets", "Ljava/util/ArrayList;");

    jobject valueTargets = env->GetObjectField(project, TargetsFieldID);

    jclass ArrayListclz = env->GetObjectClass(valueTargets);

    jmethodID ArrayListsizeMethodID = env->GetMethodID(ArrayListclz, "size", "()I");

    jmethodID ArrayListgetMethodID = env->GetMethodID(ArrayListclz, "get", "(I)Ljava/lang/Object;");

    jsize valueTargetsSize = env->CallIntMethod(valueTargets, ArrayListsizeMethodID);

    for(int i = 0; i < valueTargetsSize;i++){

        jobject valueTarget= env->CallObjectMethod(valueTargets, ArrayListgetMethodID,i);

        jclass Targetclz = env->GetObjectClass(valueTarget);

        jfieldID DyeFieldID = env->GetFieldID(Targetclz, "Dye", "Ljava/lang/String;");

        jstring Dye = (jstring)env->GetObjectField(valueTarget, DyeFieldID);

        const char *DyeStr;

        DyeStr = env->GetStringUTFChars(Dye,JNI_FALSE);

        //LOGE("The Dye type is %s \n" , DyeStr);

        if(type == 0){

            jfieldID ParamterFieldID = env->GetFieldID(Targetclz, "Paramter", "Lcom/hongshi/slansdk/model/experiment/TargetParamter;");

            jobject valueParamter= env->GetObjectField(valueTarget, ParamterFieldID);


            jclass TargetParamterClz = env->FindClass(
                    "com/hongshi/slansdk/model/experiment/TargetParamter");
//            BeginBaseline;//基线起点
//            EndBaseline;//基线终点
//            AutoThreshold;//	自动阀值
//            Threshold;//非自动阈值时，手动阀值设置值
//            OptimizationMode; //0	不优化  1	自动优化  2	手动优化
//            DigitalFilter; //0	不滤波    1	轻度滤波    2	重度滤波
            jfieldID BeginBaselineFieldID = env->GetFieldID(TargetParamterClz, "BeginBaseline", "I");
            jfieldID EndBaselineFieldID = env->GetFieldID(TargetParamterClz, "EndBaseline", "I");
            jfieldID AutoThresholdFieldID = env->GetFieldID(TargetParamterClz, "AutoThreshold", "Z");
            jfieldID ThresholdFieldID = env->GetFieldID(TargetParamterClz, "Threshold", "D");
            jfieldID OptimizationModeFieldID = env->GetFieldID(TargetParamterClz, "OptimizationMode","I");
            jfieldID DigitalFilterFieldID = env->GetFieldID(TargetParamterClz, "DigitalFilter", "I");

            env->DeleteLocalRef(TargetParamterClz);

            if (strcmp(DyeStr, "FAM") == 0) {
                capture_FAM = true;
                BeginBaseline_FAM = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_FAM = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_FAM = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_FAM = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_FAM = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_FAM = env->GetIntField(valueParamter, DigitalFilterFieldID);
                LOGE("BeginBaseline_FAM %d\n",BeginBaseline_FAM);
                LOGE("EndBaseline_FAM %d\n",EndBaseline_FAM);
                LOGE("AutoThreshold_FAM %d\n",AutoThreshold_FAM);
                LOGE("Threshold_FAM %f\n",Threshold_FAM);
                LOGE("OptimizationMode_FAM %d\n",OptimizationMode_FAM);
                LOGE("DigitalFilter_FAM %d\n",DigitalFilter_FAM);
            } else if (strcmp(DyeStr, "HEX") == 0) {
                capture_HEX = true;
                BeginBaseline_HEX = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_HEX = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_HEX = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_HEX = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_HEX = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_HEX = env->GetIntField(valueParamter, DigitalFilterFieldID);
                LOGE("BeginBaseline_HEX %d\n",BeginBaseline_HEX);
                LOGE("EndBaseline_HEX %d\n",EndBaseline_HEX);
                LOGE("AutoThreshold_HEX %d\n",AutoThreshold_HEX);
                LOGE("Threshold_HEX %f\n",Threshold_HEX);
                LOGE("OptimizationMode_HEX %d\n",OptimizationMode_HEX);
                LOGE("DigitalFilter_HEX %d\n",DigitalFilter_HEX);
            }else if (strcmp(DyeStr, "ROX") == 0) {
                capture_ROX = true;
                BeginBaseline_ROX = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_ROX = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_ROX = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_ROX = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_ROX = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_ROX = env->GetIntField(valueParamter, DigitalFilterFieldID);
                LOGE("BeginBaseline_ROX %d\n",BeginBaseline_ROX);
                LOGE("EndBaseline_ROX %d\n",EndBaseline_ROX);
                LOGE("AutoThreshold_ROX %d\n",AutoThreshold_ROX);
                LOGE("Threshold_ROX %f\n",Threshold_ROX);
                LOGE("OptimizationMode_ROX %d\n",OptimizationMode_ROX);
                LOGE("DigitalFilter_ROX %d\n",DigitalFilter_ROX);
            }else if (strcmp(DyeStr, "CY5") == 0) {
                capture_CY5 = true;
                BeginBaseline_CY5 = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_CY5 = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_CY5 = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_CY5 = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_CY5 = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_CY5 = env->GetIntField(valueParamter, DigitalFilterFieldID);
                LOGE("BeginBaseline_CY5 %d\n",BeginBaseline_CY5);
                LOGE("EndBaseline_CY5 %d\n",EndBaseline_CY5);
                LOGE("AutoThreshold_CY5 %d\n",AutoThreshold_CY5);
                LOGE("Threshold_CY5 %f\n",Threshold_CY5);
                LOGE("OptimizationMode_CY5 %d\n",OptimizationMode_CY5);
                LOGE("DigitalFilter_CY5 %d\n",DigitalFilter_CY5);
            }

        }else if(type == 1){

            jfieldID ParamterFieldID = env->GetFieldID(Targetclz, "Paramter", "Lcom/hongshi/slansdk/model/experiment/TargetParamter;");

            jobject valueParamter= env->GetObjectField(valueTarget, ParamterFieldID);

            jclass TargetParamterMeltClz = env->FindClass(
                    "com/hongshi/slansdk/model/experiment/TargetParamterMelt");
//            BeginBaseline;//基线起点
//            EndBaseline;//基线终点
//            AutoThreshold;//	自动阀值
//            Threshold;//非自动阈值时，手动阀值设置值
//            OptimizationMode; //0	不优化  1	自动优化  2	手动优化
//            DigitalFilter; //0	不滤波    1	轻度滤波    2	重度滤波
//            MinTemperature;//最小温度
//            MaxTemperature;//最大温度
//            PeekWidthThreshold;//峰宽阈值
//            PeekHightThreshold;//峰高阈值
//            PeekType; //0	正峰  1	负峰  2	正负峰
//            MeltPeakAlgorithm;//0	标准算法（以最大Y值为Tm）  1	高级算法
            jfieldID BeginBaselineFieldID = env->GetFieldID(TargetParamterMeltClz, "BeginBaseline", "I");
            jfieldID EndBaselineFieldID = env->GetFieldID(TargetParamterMeltClz, "EndBaseline", "I");
            jfieldID AutoThresholdFieldID = env->GetFieldID(TargetParamterMeltClz, "AutoThreshold", "Z");
            jfieldID ThresholdFieldID = env->GetFieldID(TargetParamterMeltClz, "Threshold", "D");
            jfieldID OptimizationModeFieldID = env->GetFieldID(TargetParamterMeltClz, "OptimizationMode","I");
            jfieldID DigitalFilterFieldID = env->GetFieldID(TargetParamterMeltClz, "DigitalFilter", "I");
            jfieldID MinTemperatureFieldID = env->GetFieldID(TargetParamterMeltClz, "MinTemperature","D");
            jfieldID MaxTemperatureFieldID = env->GetFieldID(TargetParamterMeltClz, "MaxTemperature","D");
            jfieldID PeekWidthThresholdFieldID = env->GetFieldID(TargetParamterMeltClz, "PeekWidthThreshold","D");
            jfieldID PeekHightThresholdFieldID = env->GetFieldID(TargetParamterMeltClz, "PeekHightThreshold","D");
            jfieldID PeekTypeFieldID = env->GetFieldID(TargetParamterMeltClz, "PeekType", "I");
            jfieldID MeltPeakAlgorithmFieldID = env->GetFieldID(TargetParamterMeltClz, "MeltPeakAlgorithm", "I");

            if (strcmp(DyeStr, "FAM") == 0) {
                capture_FAM = true;
                BeginBaseline_FAM = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_FAM = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_FAM = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_FAM = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_FAM = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_FAM = env->GetIntField(valueParamter, DigitalFilterFieldID);
                ///////熔解参数
                MinTemperature_FAM = env->GetDoubleField(valueParamter, MinTemperatureFieldID);;//最小温度
                MaxTemperature_FAM = env->GetDoubleField(valueParamter, MaxTemperatureFieldID);;//最大温度
                PeekWidthThreshold_FAM = env->GetDoubleField(valueParamter, PeekWidthThresholdFieldID);;//峰宽阈值
                PeekHightThreshold_FAM = env->GetDoubleField(valueParamter, PeekHightThresholdFieldID);;//峰高阈值
                PeekType_FAM = env->GetIntField(valueParamter, PeekTypeFieldID);; //0	正峰  1	负峰  2	正负峰
                MeltPeakAlgorithm_FAM = env->GetIntField(valueParamter, MeltPeakAlgorithmFieldID);;//0	标准算法（以最大Y值为Tm）  1	高级算法

                LOGE("BeginBaseline_FAM %d\n",BeginBaseline_FAM);
                LOGE("EndBaseline_FAM %d\n",EndBaseline_FAM);
                LOGE("AutoThreshold_FAM %d\n",AutoThreshold_FAM);
                LOGE("Threshold_FAM %f\n",Threshold_FAM);
                LOGE("OptimizationMode_FAM %d\n",OptimizationMode_FAM);
                LOGE("DigitalFilter_FAM %d\n",DigitalFilter_FAM);
                /////熔解参数
                LOGE("MinTemperature_FAM %d\n",MinTemperature_FAM);
                LOGE("MaxTemperature_FAM %d\n",MaxTemperature_FAM);
                LOGE("PeekWidthThreshold_FAM %d\n",PeekWidthThreshold_FAM);
                LOGE("PeekHightThreshold_FAM %f\n",PeekHightThreshold_FAM);
                LOGE("PeekType_FAM %d\n",PeekType_FAM);
                LOGE("MeltPeakAlgorithm_FAM %d\n",MeltPeakAlgorithm_FAM);

            } else if (strcmp(DyeStr, "HEX") == 0) {
                capture_HEX = true;
                BeginBaseline_HEX = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_HEX = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_HEX = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_HEX = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_HEX = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_HEX = env->GetIntField(valueParamter, DigitalFilterFieldID);
                ///////熔解参数
                MinTemperature_HEX = env->GetDoubleField(valueParamter, MinTemperatureFieldID);;//最小温度
                MaxTemperature_HEX = env->GetDoubleField(valueParamter, MaxTemperatureFieldID);;//最大温度
                PeekWidthThreshold_HEX = env->GetDoubleField(valueParamter, PeekWidthThresholdFieldID);;//峰宽阈值
                PeekHightThreshold_HEX = env->GetDoubleField(valueParamter, PeekHightThresholdFieldID);;//峰高阈值
                PeekType_HEX = env->GetIntField(valueParamter, PeekTypeFieldID);; //0	正峰  1	负峰  2	正负峰
                MeltPeakAlgorithm_HEX = env->GetIntField(valueParamter, MeltPeakAlgorithmFieldID);;//0	标准算法（以最大Y值为Tm）  1	高级算法

                LOGE("BeginBaseline_HEX %d\n",BeginBaseline_HEX);
                LOGE("EndBaseline_HEX %d\n",EndBaseline_HEX);
                LOGE("AutoThreshold_HEX %d\n",AutoThreshold_HEX);
                LOGE("Threshold_HEX %f\n",Threshold_HEX);
                LOGE("OptimizationMode_HEX %d\n",OptimizationMode_HEX);
                LOGE("DigitalFilter_HEX %d\n",DigitalFilter_HEX);
                /////熔解参数
                LOGE("MinTemperature_HEX %d\n",MinTemperature_HEX);
                LOGE("MaxTemperature_HEX %d\n",MaxTemperature_HEX);
                LOGE("PeekWidthThreshold_HEX %d\n",PeekWidthThreshold_HEX);
                LOGE("PeekHightThreshold_HEX %f\n",PeekHightThreshold_HEX);
                LOGE("PeekType_HEX %d\n",PeekType_HEX);
                LOGE("MeltPeakAlgorithm_HEX %d\n",MeltPeakAlgorithm_HEX);
            }else if (strcmp(DyeStr, "ROX") == 0) {
                capture_ROX = true;
                BeginBaseline_ROX = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_ROX = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_ROX = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_ROX = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_ROX = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_ROX = env->GetIntField(valueParamter, DigitalFilterFieldID);
                ///////熔解参数
                MinTemperature_ROX = env->GetDoubleField(valueParamter, MinTemperatureFieldID);;//最小温度
                MaxTemperature_ROX = env->GetDoubleField(valueParamter, MaxTemperatureFieldID);;//最大温度
                PeekWidthThreshold_ROX = env->GetDoubleField(valueParamter, PeekWidthThresholdFieldID);;//峰宽阈值
                PeekHightThreshold_ROX = env->GetDoubleField(valueParamter, PeekHightThresholdFieldID);;//峰高阈值
                PeekType_ROX = env->GetIntField(valueParamter, PeekTypeFieldID);; //0	正峰  1	负峰  2	正负峰
                MeltPeakAlgorithm_ROX = env->GetIntField(valueParamter, MeltPeakAlgorithmFieldID);;//0	标准算法（以最大Y值为Tm）  1	高级算法

                LOGE("BeginBaseline_ROX %d\n",BeginBaseline_ROX);
                LOGE("EndBaseline_ROX %d\n",EndBaseline_ROX);
                LOGE("AutoThreshold_ROX %d\n",AutoThreshold_ROX);
                LOGE("Threshold_ROX %f\n",Threshold_ROX);
                LOGE("OptimizationMode_ROX %d\n",OptimizationMode_ROX);
                LOGE("DigitalFilter_ROX %d\n",DigitalFilter_ROX);
                /////熔解参数
                LOGE("MinTemperature_ROX %d\n",MinTemperature_ROX);
                LOGE("MaxTemperature_ROX %d\n",MaxTemperature_ROX);
                LOGE("PeekWidthThreshold_ROX %d\n",PeekWidthThreshold_ROX);
                LOGE("PeekHightThreshold_ROX %f\n",PeekHightThreshold_ROX);
                LOGE("PeekType_ROX %d\n",PeekType_ROX);
                LOGE("MeltPeakAlgorithm_ROX %d\n",MeltPeakAlgorithm_ROX);
            }else if (strcmp(DyeStr, "CY5") == 0) {
                capture_CY5 = true;
                BeginBaseline_CY5 = env->GetIntField(valueParamter, BeginBaselineFieldID);
                EndBaseline_CY5 = env->GetIntField(valueParamter, EndBaselineFieldID);
                AutoThreshold_CY5 = env->GetBooleanField(valueParamter, AutoThresholdFieldID);
                Threshold_CY5 = env->GetDoubleField(valueParamter, ThresholdFieldID);
                OptimizationMode_CY5 = env->GetIntField(valueParamter, OptimizationModeFieldID);
                DigitalFilter_CY5 = env->GetIntField(valueParamter, DigitalFilterFieldID);
                ///////熔解参数
                MinTemperature_CY5 = env->GetDoubleField(valueParamter, MinTemperatureFieldID);;//最小温度
                MaxTemperature_CY5 = env->GetDoubleField(valueParamter, MaxTemperatureFieldID);;//最大温度
                PeekWidthThreshold_CY5 = env->GetDoubleField(valueParamter, PeekWidthThresholdFieldID);;//峰宽阈值
                PeekHightThreshold_CY5 = env->GetDoubleField(valueParamter, PeekHightThresholdFieldID);;//峰高阈值
                PeekType_CY5 = env->GetIntField(valueParamter, PeekTypeFieldID);; //0	正峰  1	负峰  2	正负峰
                MeltPeakAlgorithm_CY5 = env->GetIntField(valueParamter, MeltPeakAlgorithmFieldID);;//0	标准算法（以最大Y值为Tm）  1	高级算法

                LOGE("BeginBaseline_CY5 %d\n",BeginBaseline_CY5);
                LOGE("EndBaseline_CY5 %d\n",EndBaseline_CY5);
                LOGE("AutoThreshold_CY5 %d\n",AutoThreshold_CY5);
                LOGE("Threshold_CY5 %f\n",Threshold_CY5);
                LOGE("OptimizationMode_CY5 %d\n",OptimizationMode_CY5);
                LOGE("DigitalFilter_CY5 %d\n",DigitalFilter_CY5);
                /////熔解参数
                LOGE("MinTemperature_CY5 %d\n",MinTemperature_CY5);
                LOGE("MaxTemperature_CY5 %d\n",MaxTemperature_CY5);
                LOGE("PeekWidthThreshold_CY5 %d\n",PeekWidthThreshold_CY5);
                LOGE("PeekHightThreshold_CY5 %f\n",PeekHightThreshold_CY5);
                LOGE("PeekType_CY5 %d\n",PeekType_CY5);
                LOGE("MeltPeakAlgorithm_CY5 %d\n",MeltPeakAlgorithm_CY5);
            }
            env->DeleteLocalRef(TargetParamterMeltClz);
        }
        env->DeleteLocalRef(Targetclz);
    }



//    return env->NewStringUTF(a);

    LOGE("begin wellResultListSize");

    jobject wellResultList = (jobject) env->GetObjectField(experiment, wellResultFieldID); // 获取字段对应的值

    jclass listClass = env->FindClass("java/util/List");
    jmethodID getMethodID = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
    jmethodID sizeMethodID = env->GetMethodID(listClass, "size", "()I");
    jmethodID toArrayMethodID = env->GetMethodID(listClass, "toArray", "()[Ljava/lang/Object;");
    jmethodID clearMethodID = env->GetMethodID(listClass, "clear", "()V");
    jmethodID addMethodID = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

    jclass DoubleClass = env->FindClass("java/lang/Double");
    jmethodID doubleValueID = env->GetMethodID(DoubleClass, "doubleValue", "()D");
    jmethodID DoubleConstructorID = env->GetMethodID(DoubleClass, "<init>", "(D)V");
    jsize wellResultListSize = env->CallIntMethod(wellResultList, sizeMethodID);

    LOGE("The wellResultListSize is %d" , wellResultListSize);

    for(int i = 0; i < wellResultListSize;i++){

        jobject valuewellResult = (jobjectArray)env->CallObjectMethod(wellResultList, getMethodID,i);

        jclass wellResultClz = env->GetObjectClass(valuewellResult);

        jfieldID IDFieldID = env->GetFieldID(wellResultClz, "ID", "Ljava/lang/String;");

        jstring wellResult_ID = (jstring) env->GetObjectField(valuewellResult, IDFieldID);

        const char *str;

        str = env->GetStringUTFChars(wellResult_ID, JNI_FALSE);

        //LOGI("The wellResult_ID is %s\n" , str);


        jfieldID TargetResultsFieldID = env->GetFieldID(wellResultClz, "TargetResults", "Ljava/util/List;");

        jobject TargetResultsList = (jobject) env->GetObjectField(valuewellResult, TargetResultsFieldID); // 获取字段对应的值

        jsize TargetResultsListSize = env->CallIntMethod(TargetResultsList, sizeMethodID);

        //LOGI("The TargetResultsListSize is %d\n" , TargetResultsListSize);

        env->DeleteLocalRef(wellResultClz);

        for(int j = 0; j < TargetResultsListSize;j++){

            jobject valueTargetResult = (jobjectArray)env->CallObjectMethod(TargetResultsList, getMethodID,j);

            jclass targetResultClz = env->GetObjectClass(valueTargetResult);

            jfieldID targetResultNameFieldID = env->GetFieldID(targetResultClz, "Name", "Ljava/lang/String;");

            jstring targetResultNameField = (jstring) env->GetObjectField(valueTargetResult, targetResultNameFieldID);

            const char *targetResultNameStr;

            targetResultNameStr = env->GetStringUTFChars(targetResultNameField, JNI_FALSE);

            //LOGI("The targetResultName is %s\n" , targetResultNameStr);

            ////////处理AMP曲线---------------------------
            ////////处理AMP曲线---------------------------
            ////////处理AMP曲线---------------------------

            jfieldID RawCurveID = env->GetFieldID(targetResultClz, "RawCurve", "Ljava/util/List;");

            jobject valueRawCurve = (jobject) env->GetObjectField(valueTargetResult, RawCurveID);

            jsize TargetResultsRawCurveListSize = env->CallIntMethod(valueRawCurve, sizeMethodID);

            //LOGI("The RawCurve length is %d\n" , TargetResultsRawCurveListSize);

            if(TargetResultsRawCurveListSize > 12 ){
                ////////////////////根据通道DYE读入分析参数
                jfieldID DyeFieldID = env->GetFieldID(targetResultClz, "Dye", "Ljava/lang/String;");

                jstring Dye = (jstring)env->GetObjectField(valueTargetResult, DyeFieldID);

                const char *DyeStr;

                DyeStr = env->GetStringUTFChars(Dye,JNI_FALSE);

                /////有默认值
                int BeginBaseline = 6;//基线起点
                int EndBaseline = 10 ;//基线终点
                bool AutoThreshold = 0.12;//	自动阀值
                double Threshold = 0.12;//非自动阈值时，手动阀值设置值
                int OptimizationMode = 1; //0	不优化  1	自动优化  2	手动优化
                int DigitalFilter = 1 ; //0	不滤波    1	轻度滤波    2	重度滤波

                if (strcmp(DyeStr, "FAM") == 0) {
                    if(!capture_FAM){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_FAM;
                    EndBaseline = EndBaseline_FAM;
                    AutoThreshold = AutoThreshold_FAM;
                    Threshold = Threshold_FAM;
                    OptimizationMode = OptimizationMode_FAM;
                    DigitalFilter = DigitalFilter_FAM;
                } else if (strcmp(DyeStr, "HEX") == 0) {
                    if(!capture_HEX){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_HEX;
                    EndBaseline = EndBaseline_HEX;
                    AutoThreshold = AutoThreshold_HEX;
                    Threshold = Threshold_HEX;
                    OptimizationMode = OptimizationMode_HEX;
                    DigitalFilter = DigitalFilter_HEX;
                }else if (strcmp(DyeStr, "ROX") == 0) {
                    if(!capture_ROX){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_ROX;
                    EndBaseline = EndBaseline_ROX;
                    AutoThreshold = AutoThreshold_ROX;
                    Threshold = Threshold_ROX;
                    OptimizationMode = OptimizationMode_ROX;
                    DigitalFilter = DigitalFilter_ROX;
                }else if (strcmp(DyeStr, "CY5") == 0) {
                    if(!capture_CY5){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_CY5;
                    EndBaseline = EndBaseline_CY5;
                    AutoThreshold = AutoThreshold_CY5;
                    Threshold = Threshold_CY5;
                    OptimizationMode = OptimizationMode_CY5;
                    DigitalFilter = DigitalFilter_CY5;
                }

                double* RawCurveArray = (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));
                double* x = (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));
                for(int k = 0; k < TargetResultsRawCurveListSize;k++){
                    jobject ValueDouble= env->CallObjectMethod(valueRawCurve, getMethodID,k);
                    *(RawCurveArray+ k) = env->CallDoubleMethod(ValueDouble, doubleValueID);
                    *(x + k) = k+1;
                    LOGI("The RawCurveArray[%d] length is %f\n" , k ,RawCurveArray[k]);
                }
                double* filtedData = (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));

                if(DigitalFilter == 0 ){
                    filter(RawCurveArray, TargetResultsRawCurveListSize, 3,3, 1,1,filtedData);//滤波
                }else if(DigitalFilter == 1 ){
                    filter(RawCurveArray, TargetResultsRawCurveListSize, 3,3, 1,1,filtedData);//滤波
                }if(DigitalFilter == 2 ){
                    filter(RawCurveArray, TargetResultsRawCurveListSize, 3,3, 1,2,filtedData);//滤波
                }

                //int* x_index =  (int*)malloc(TargetResultsRawCurveListSize * sizeof(int));
                double* array_baseline_detrend = (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));

                //基线矫正：选择区间线性拟合+去趋势化
                baseline_detrend(x, filtedData, TargetResultsRawCurveListSize, BeginBaseline, EndBaseline, array_baseline_detrend);

                LOGI("array_baseline_detrend{BeginBaseline,EndBaseline} , %d --- %d", BeginBaseline, EndBaseline);

                double* delRn =  (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));//相对荧光

                double* normal =  (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));//标准化线性

                double* log_normal =  (double*)malloc(TargetResultsRawCurveListSize * sizeof(double));//标准化log

                for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                {
                    LOGI("array_baseline_detrend{0} , %d --- %f", i, *(array_baseline_detrend + i));
                }
                LOGI("---------------------------");
                double baseline = find_baseline(array_baseline_detrend, BeginBaseline, EndBaseline);
                LOGI("BeginBaseline---------------------------%d",BeginBaseline);
                LOGI("EndBaseline---------------------------%d",EndBaseline);
                LOGI("baseline---------------------------%f",baseline);
                if(baseline == 0)
                {
                    baseline = baseline + 1;
                    LOGI("baseline---reset------------------------%f",baseline);
                }

                deltaRn(array_baseline_detrend, TargetResultsRawCurveListSize, baseline, delRn);
                linear_normalize(delRn, TargetResultsRawCurveListSize, baseline, normal);
                log_normalize(delRn, TargetResultsRawCurveListSize, baseline, log_normal);
                for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                {
                    LOGI("normal{0} , %d --- %f", i, *(normal + i));
                }
                LOGI("---------------------------");
                for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                {
                    LOGI("log_normal{0} , %d --- %f", i, *(log_normal + i));
                }


                jfieldID AMPCurveID = env->GetFieldID(targetResultClz, "AMPCurve", "Ljava/util/List;");

                jobject valueAMPCurve = (jobject) env->GetObjectField(valueTargetResult, AMPCurveID);

                env->CallVoidMethod(valueAMPCurve, clearMethodID);


                if(AMPAlgorithm == 0 ){
                    for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                    {
                        jobject normalDouble = env->NewObject(DoubleClass, DoubleConstructorID,log_normal[i]);

                        env->CallBooleanMethod(valueAMPCurve, addMethodID,normalDouble);
                    }
                }else if (AMPAlgorithm == 1 ){
                    for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                    {
                        jobject normalDouble = env->NewObject(DoubleClass, DoubleConstructorID,delRn[i]);

                        env->CallBooleanMethod(valueAMPCurve, addMethodID,normalDouble);
                    }
                }else if (AMPAlgorithm == 2 ){
                    for (int i = 0; i < TargetResultsRawCurveListSize; i++)
                    {
                        jobject normalDouble = env->NewObject(DoubleClass, DoubleConstructorID,normal[i]);

                        env->CallBooleanMethod(valueAMPCurve, addMethodID,normalDouble);
                    }
                }



                double threshold_auto = auto_threshold(normal, BeginBaseline, EndBaseline);
                double ct_auto = find_ct(x, normal, TargetResultsRawCurveListSize, threshold_auto, 0.01);

                double threshold_manual = Threshold;
                double ct_manual = find_ct(x, normal, TargetResultsRawCurveListSize, threshold_manual, 0.01);

                LOGE("The %s %s baseline is %f" ,str , targetResultNameStr ,baseline);
                LOGE("The %s %s threshold_auto is %f" ,str, targetResultNameStr,threshold_auto);
                LOGE("The %s %s ct_auto is %f" ,str, targetResultNameStr ,ct_auto);

                LOGE("The %s %s threshold_manual is %f" ,str, targetResultNameStr,threshold_manual);
                LOGE("The %s %s ct_manual is %f" ,str, targetResultNameStr ,ct_manual);

                jfieldID CtId = env->GetFieldID(targetResultClz, "Ct", "D");
                jfieldID CtMeanId = env->GetFieldID(targetResultClz, "CtMean", "D");

                env->SetDoubleField(valueTargetResult, CtId, ct_manual);
                env->SetDoubleField(valueTargetResult, CtMeanId, ct_auto);

                jfieldID CnId = env->GetFieldID(targetResultClz, "Cn", "D");
                jfieldID CnMeanId = env->GetFieldID(targetResultClz, "CnMean", "D");
                env->SetDoubleField(valueTargetResult, CnId, threshold_manual);
                env->SetDoubleField(valueTargetResult, CnMeanId, threshold_auto);

                free(RawCurveArray);
                free(x);
                free(filtedData);
                free(delRn);
                free(normal);
                free(log_normal);
                //free(x_index);
                free(array_baseline_detrend);

                RawCurveArray = NULL;
                x = NULL;
                filtedData = NULL;
                delRn = NULL;
                normal = NULL;
                log_normal = NULL;
                //x_index = NULL;
                array_baseline_detrend = NULL;
            }

            ////////处理Melt曲线---------------------------
            ////////处理Melt曲线---------------------------
            ////////处理Melt曲线---------------------------
            jfieldID RawMeltCurveID = env->GetFieldID(targetResultClz, "RawMeltCurve", "Ljava/util/List;");

            jobject valueRawMeltCurve = (jobject) env->GetObjectField(valueTargetResult, RawMeltCurveID);

            jfieldID PeakCurveID = env->GetFieldID(targetResultClz, "PeakCurve", "Ljava/util/List;");

            jobject valuePeakCurve = (jobject) env->GetObjectField(valueTargetResult, PeakCurveID);

            jsize TargetResultsRawMeltCurveListSize = env->CallIntMethod(valueRawMeltCurve, sizeMethodID);

            //LOGI("The RawMeltCurve length is %d\n" , TargetResultsRawMeltCurveListSize);

            env->CallVoidMethod(valuePeakCurve, clearMethodID);

            if(TargetResultsRawMeltCurveListSize >= 150){

                ////////////////////根据通道DYE读入分析参数
                jfieldID DyeFieldID = env->GetFieldID(targetResultClz, "Dye", "Ljava/lang/String;");

                jstring Dye = (jstring)env->GetObjectField(valueTargetResult, DyeFieldID);

                const char *DyeStr;

                DyeStr = env->GetStringUTFChars(Dye,JNI_FALSE);

                /////有默认值
                int BeginBaseline;//基线起点
                int EndBaseline;//基线终点
                bool AutoThreshold;//	自动阀值
                double Threshold;//非自动阈值时，手动阀值设置值
                int OptimizationMode; //0	不优化  1	自动优化  2	手动优化
                int DigitalFilter; //0	不滤波    1	轻度滤波    2	重度滤波
/////////////////////////melt
                double MinTemperature;//最小温度
                double MaxTemperature;//最大温度
                double PeekWidthThreshold;//峰宽阈值
                double PeekHightThreshold;//峰高阈值
                int PeekType; //0	正峰  1	负峰  2	正负峰
                int MeltPeakAlgorithm;//0	标准算法（以最大Y值为Tm）  1	高级算法

                if (strcmp(DyeStr, "FAM") == 0) {
                    if(!capture_FAM){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_FAM;
                    EndBaseline = EndBaseline_FAM;
                    AutoThreshold = AutoThreshold_FAM;
                    Threshold = Threshold_FAM;
                    OptimizationMode = OptimizationMode_FAM;
                    DigitalFilter = DigitalFilter_FAM;
                    MinTemperature = MinTemperature_FAM;//最小温度
                    MaxTemperature = MaxTemperature_FAM;//最大温度
                    PeekWidthThreshold = PeekWidthThreshold_FAM;//峰宽阈值
                    PeekHightThreshold = PeekHightThreshold_FAM;//峰高阈值
                    PeekType = PeekType_FAM; //0	正峰  1	负峰  2	正负峰
                    MeltPeakAlgorithm = MeltPeakAlgorithm_FAM;//0	标准算法（以最大Y值为Tm）  1	高级算法
                } else if (strcmp(DyeStr, "HEX") == 0) {
                    if(!capture_HEX){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_HEX;
                    EndBaseline = EndBaseline_HEX;
                    AutoThreshold = AutoThreshold_HEX;
                    Threshold = Threshold_HEX;
                    OptimizationMode = OptimizationMode_HEX;
                    DigitalFilter = DigitalFilter_HEX;
                    MinTemperature = MinTemperature_HEX;//最小温度
                    MaxTemperature = MaxTemperature_HEX;//最大温度
                    PeekWidthThreshold = PeekWidthThreshold_HEX;//峰宽阈值
                    PeekHightThreshold = PeekHightThreshold_HEX;//峰高阈值
                    PeekType = PeekType_HEX; //0	正峰  1	负峰  2	正负峰
                    MeltPeakAlgorithm = MeltPeakAlgorithm_HEX;//0	标准算法（以最大Y值为Tm）  1	高级算法
                }else if (strcmp(DyeStr, "ROX") == 0) {
                    if(!capture_ROX){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_ROX;
                    EndBaseline = EndBaseline_ROX;
                    AutoThreshold = AutoThreshold_ROX;
                    Threshold = Threshold_ROX;
                    OptimizationMode = OptimizationMode_ROX;
                    DigitalFilter = DigitalFilter_ROX;
                    MinTemperature = MinTemperature_ROX;//最小温度
                    MaxTemperature = MaxTemperature_ROX;//最大温度
                    PeekWidthThreshold = PeekWidthThreshold_ROX;//峰宽阈值
                    PeekHightThreshold = PeekHightThreshold_ROX;//峰高阈值
                    PeekType = PeekType_ROX; //0	正峰  1	负峰  2	正负峰
                    MeltPeakAlgorithm = MeltPeakAlgorithm_FAM;//0	标准算法（以最大Y值为Tm）  1	高级算法
                }else if (strcmp(DyeStr, "CY5") == 0) {
                    if(!capture_CY5){
                        continue;
                    }
                    BeginBaseline = BeginBaseline_CY5;
                    EndBaseline = EndBaseline_CY5;
                    AutoThreshold = AutoThreshold_CY5;
                    Threshold = Threshold_CY5;
                    OptimizationMode = OptimizationMode_CY5;
                    DigitalFilter = DigitalFilter_CY5;
                    MinTemperature = MinTemperature_CY5;//最小温度
                    MaxTemperature = MaxTemperature_CY5;//最大温度
                    PeekWidthThreshold = PeekWidthThreshold_CY5;//峰宽阈值
                    PeekHightThreshold = PeekHightThreshold_CY5;//峰高阈值
                    PeekType = PeekType_CY5; //0	正峰  1	负峰  2	正负峰
                    MeltPeakAlgorithm = MeltPeakAlgorithm_CY5;//0	标准算法（以最大Y值为Tm）  1	高级算法
                }

                double* x = (double*)malloc(TargetResultsRawMeltCurveListSize * sizeof(double));
                double* testData = (double*)malloc(TargetResultsRawMeltCurveListSize * sizeof(double));

                jobject objectDot= env->CallObjectMethod(valueRawMeltCurve, getMethodID,0);
                jclass DotClass = env->GetObjectClass(objectDot);
                jfieldID XFieldID = env->GetFieldID(DotClass, "X", "D");
                jfieldID YFieldID = env->GetFieldID(DotClass, "Y", "D");

//                jmethodID DotConstructorID = env->GetMethodID(DotClass, "Dot", "(D)V");

                for(int i = 0; i < TargetResultsRawMeltCurveListSize;i++){
                    jobject ValueDot= env->CallObjectMethod(valueRawMeltCurve, getMethodID,i);
                    *(x + i) = env->GetDoubleField(ValueDot, XFieldID);
                    *(testData + i) = env->GetDoubleField(ValueDot, YFieldID);
                    ////LOGE("The DOT %d ---- X [%.2f]  and Y is [%.2f]\n" ,i, *(x + i), *(testData + i));
                }





                //////////////////////loess新处理
                //重复值处理
                int Xlength = TargetResultsRawMeltCurveListSize;
                deduplication(x, Xlength);
                //double precision = interval;
                double precision = 0.1;
                //LOGE("The Xlength = %d \n",Xlength);
                int knots = resampling(x,Xlength,precision);
                //LOGE("The knots =  %d\n",knots);
                //插值重采样
                double* x_interp_data = (double*)malloc(knots * sizeof(double));
                double* y_interp_data = (double*)malloc(knots * sizeof(double));
                interpolation(x, testData, Xlength, x_interp_data, y_interp_data, precision);
                //差分前滤波
                int width = knots / 5;

                if (precision == 0.1)
                {
                    width = 100;
                }
                else if (precision == 0.05)
                {
                    width = 200;
                }
                else if (precision == 0.2)
                {
                    width = 50;
                }
                LOGE("The loess width = %d \n",width);
                double* pre_filtedData = (double*)malloc(knots * sizeof(double));
                LoessSmoother* smoother = create_loess_smoother(y_interp_data, knots, width, 1);//记得destroy
                loess_smoother_smooth(smoother, pre_filtedData);
                //程序结束前释放空间：
                destroy_loess_smoother(smoother);

                //差分
                double* derived_data = (double*)malloc(knots * sizeof(double));
                derivation_step(x_interp_data, pre_filtedData, knots, derived_data, 1);
                //差分后滤波
                double* last_filtedData = (double*)malloc(knots * sizeof(double));
                //filter(derived_data, knots, 21, 21, 1, 3, last_filtedData);

                int width_post = knots / 5;
                if (precision == 0.1)
                {
                    width_post = 100;
                }
                else if (precision == 0.05)
                {
                    width_post = 200;
                }
                else if (precision == 0.2)
                {
                    width_post = 50;
                }

                LoessSmoother* smoother_post = create_loess_smoother(derived_data, knots, width_post, 1);//记得destroy
                loess_smoother_smooth(smoother_post, last_filtedData);
                //程序结束前释放空间：
                destroy_loess_smoother(smoother_post);
                //打印输出

//                for (int row = 0; row < knots; row++)
//                {
//                    jobject peakDot = env->AllocObject(DotClass);
//                    env->SetDoubleField(peakDot, XFieldID, *(x_interp_data + row));
//                    env->SetDoubleField(peakDot, YFieldID, (*(last_filtedData + row)));
//
//                    env->CallBooleanMethod(valuePeakCurve, addMethodID,peakDot);
//                }
                //计算符合条件的峰值
                //int count = peakcount(last_filtedData, knots);
                int count =1000;
                LOGE("peakcount %d\n",count);
                int*  melting_index;
                double* Tm;
                double* height;
                double* relheight;
                if(count == 0){
                    melting_index = (int*)malloc(1000 * sizeof(int));
                    Tm = (double*)malloc(1000 * sizeof(double));
                    height = (double*)malloc(1000 * sizeof(double));
                    relheight = (double*)malloc(1000 * sizeof(double));
                }else{
                    melting_index = (int*)malloc(count * sizeof(int));
                    Tm = (double*)malloc(count * sizeof(double));
                    height = (double*)malloc(count * sizeof(double));
                    relheight = (double*)malloc(count * sizeof(double));
                }
                //寻峰
                //peakdetecter(x_interp_data, last_filtedData, knots, melting_index, Tm, height, relheight);
//                bool optimization_enabl_FAM = true;
//                double enhance_lower_FAM = 2;
//                double enhance_upper_FAM = 5;
//                double enhance_gain_FAM = 1.5;
//                double weaken_FAM = 2;
//                double weaken_gain_FAM = 0.5;
//                double slope_thres_FAM = 0.3;

                int peakCount = 0;
                if (strcmp(DyeStr, "FAM") == 0 && count != 0) {
                    peakCount = peakdetecter_optimization(x_interp_data, last_filtedData, knots, melting_index, Tm, height,relheight,
                                              optimization_enabl_FAM, enhance_lower_FAM * 10, enhance_upper_FAM * 10, enhance_gain_FAM, weaken_FAM * 10,weaken_gain_FAM, slope_thres_FAM );
                }else if (strcmp(DyeStr, "HEX") == 0 && count != 0) {
                    peakCount = peakdetecter_optimization(x_interp_data, last_filtedData, knots, melting_index, Tm, height,relheight,
                                              optimization_enabl_HEX, enhance_lower_HEX * 10, enhance_upper_HEX * 10, enhance_gain_HEX, weaken_HEX * 10,weaken_gain_HEX, slope_thres_HEX );

                }else if (strcmp(DyeStr, "ROX") == 0 && count != 0) {
                    peakCount = peakdetecter_optimization(x_interp_data, last_filtedData, knots, melting_index, Tm, height,relheight,
                                              optimization_enabl_ROX, enhance_lower_ROX * 10, enhance_upper_ROX * 10, enhance_gain_ROX, weaken_ROX * 10,weaken_gain_ROX, slope_thres_ROX );

                }else if (strcmp(DyeStr, "CY5") == 0 && count != 0) {
                    peakCount = peakdetecter_optimization(x_interp_data, last_filtedData, knots, melting_index, Tm, height,relheight,
                                              optimization_enabl_CY5, enhance_lower_CY5 * 10, enhance_upper_CY5 * 10, enhance_gain_CY5, weaken_CY5 * 10,weaken_gain_CY5, slope_thres_CY5 );
                }


                for (int row = 0; row < knots; row++)
                {
                    jobject peakDot = env->AllocObject(DotClass);
                    env->SetDoubleField(peakDot, XFieldID, *(x_interp_data + row));
                    env->SetDoubleField(peakDot, YFieldID, (*(last_filtedData + row))/10);
                    env->CallBooleanMethod(valuePeakCurve, addMethodID,peakDot);
                }

                env->DeleteLocalRef(DotClass);

                //设置相对峰高阈值为100，只有relheight>100的峰会被输出
                double relh_threshold = PeekHightThreshold;
                ////////存储peak结果---------------------------
                jfieldID PeakValuesID = env->GetFieldID(targetResultClz, "PeakValues", "Ljava/util/List;");
                jobject valuePeakValues = (jobject) env->GetObjectField(valueTargetResult, PeakValuesID);
                env->CallVoidMethod(valuePeakValues, clearMethodID);
                jclass PeakClass = env->FindClass(
                        "com/winters/boweilibrary/BoWei/ExperimentData/Peak");
                for (int i = 0; i < peakCount; i++)
                {
                    // 检查当前relheight是否满足条件
                    if (relheight[i]/10 >= relh_threshold)
                    {
                        // 如果满足，输出对应的Tm和height
                        //LOGE("num-%i Tm: %.2f, Height: %.2f, relheight: %.2f",i, Tm[i], height[i], relheight[i]);
                        ////////存储peak结果---------------------------
                        ////////存储peak结果---------------------------



                        jobject peakvalue = env->AllocObject(PeakClass);
                        jfieldID TmFieldID = env->GetFieldID(PeakClass, "Tm", "D");
                        jfieldID RmFieldID = env->GetFieldID(PeakClass, "Rm", "D");
                        jfieldID YmFieldID = env->GetFieldID(PeakClass, "Ym", "D");
                        jfieldID WmFieldID = env->GetFieldID(PeakClass, "Wm", "D");
                        env->SetDoubleField(peakvalue, TmFieldID, Tm[i]);
                        env->SetDoubleField(peakvalue, RmFieldID, relheight[i]/10);
                        env->SetDoubleField(peakvalue, YmFieldID, height[i]);
                        env->CallBooleanMethod(valuePeakValues, addMethodID,peakvalue);

                    }

                }
                env->DeleteLocalRef(PeakClass);



                env->DeleteLocalRef(targetResultClz);

                free(x_interp_data);
                free(y_interp_data);
                free(pre_filtedData);
                free(derived_data);
                free(last_filtedData);
                free(melting_index);
                free(Tm);
                free(height);
                free(relheight);
                free(x);
                free(testData);

                x_interp_data = NULL;
                y_interp_data = NULL;
                pre_filtedData = NULL;
                derived_data = NULL;
                last_filtedData = NULL;
                melting_index = NULL;
                Tm = NULL;
                height = NULL;
                relheight = NULL;
                x = NULL;
                testData = NULL;

            }
        }

    }

    env->DeleteLocalRef(clz);
    env->DeleteLocalRef(projectclz);
    env->DeleteLocalRef(valueProgramclz);
    env->DeleteLocalRef(ArrayListvalueSectionsclz);
    env->DeleteLocalRef(ArrayListclz);
    env->DeleteLocalRef(listClass);
    env->DeleteLocalRef(DoubleClass);
    //env->DeleteGoRef(DoubleClass);
    LOGI("The end is %d" , 22222);
    //return env->NewStringUTF(a);
}

//均值滤波
int average_filter(double *input_data, int data_length, int window_size, double *output_data) {
    int i, j, window_offset;
    if (window_size < 0) {
        window_size = 1;
    }
    if (window_size % 2 - 1) {
        window_size++;//如果输入为偶数,自动++
    }
    window_offset = (window_size - 1) / 2;
    for (i = 0; i < data_length; i++) {
        int start, end, n;
        double sum, average;
        //起止下标
        start = i - window_offset;
        end = i + window_offset;
        sum = 0;
        average = 0;
        //建立窗口数组
        double *window = (double *) malloc(window_size * sizeof(double));
        if (window == NULL) {
            //LOGE(" average_filterMemory allocation failed.\n");
            return -1;
        } // 检验非空
        double start_step =
                (*(input_data) - *(input_data + window_offset)) / (window_offset - window_size + 1);
        double end_step = (*(input_data + data_length - 1) -
                           *(input_data + data_length - 1 - window_offset)) / window_offset;
        for (j = 0; j < window_size; j++) {
            //边缘处理 拟合
            if ((i < window_offset) && (j < window_offset - i))
                *(window + j) = *(input_data) + (j - window_offset + i) * start_step;
            else if ((i > data_length - 1 - window_offset) &&
                     (j > data_length - 1 - i + window_offset))
                *(window + j) = *(input_data + data_length - 1) +
                                (j - window_offset - (data_length - 1 - i)) * end_step;
            else
                *(window + j) = *(input_data + start + j);
        }
        for (n = 0; n < window_size; n++) {
            sum += *(window + n);
        }
        average = sum / window_size;
        *(output_data + i) = average;
        free(window);
        window = NULL;
    }
    return 1;
}


//中值滤波
int median_filter(double* input_data, int data_length, int window_size, double* output_data)
{
    int i, j, window_offset;
    if (window_size < 0)
    {
        window_size = 1;
    }
    if (window_size % 2 - 1)
    {
        window_size++;//如果输入为偶数,自动++
    }
    window_offset = (window_size - 1) / 2;
    //取局部窗口
    for (i = 0;i < data_length;i++)
    {
        int start, end, n, k;
        double temp, median;
        //起止下标
        start = i - window_offset;
        end = i + window_offset;
        //建立窗口数组
        double* window = (double*)malloc(window_size * sizeof(double));
        if (window == NULL) {LOGE("median_filter  Memory allocation failed.\n");return -1; } // 检验非空
        double start_step = (*(input_data)-*(input_data + window_offset)) / (window_offset - window_size + 1);
        double end_step = (*(input_data + data_length - 1) - *(input_data + data_length - 1 - window_offset)) / window_offset;
        for (j = 0;j < window_size;j++)
        {
            //边缘处理 拟合
            if ((i < window_offset) && (j < window_offset - i))
                *(window + j) = *(input_data)+(j - window_offset + i) * start_step;
            else if ((i > data_length - 1 - window_offset) && (j > data_length - 1 - i + window_offset))
                *(window + j) = *(input_data + data_length - 1) + (j - window_offset - (data_length - 1 - i)) * end_step;
            else
                *(window + j) = *(input_data + start + j);
        }
        //对窗口局部数据window排序
        for (n = 0;n < window_size - 1;n++)
        {
            for (k = 0;k < window_size - 1 - n; k++)
            {
                if (*(window + k) > *(window + k + 1))
                {
                    temp = *(window + k);
                    *(window + k) = *(window + k + 1);
                    *(window + k + 1) = temp;
                }
            }
        }
        median = *(window + window_size / 2);
        *(output_data + i) = median;
        free(window);
        window = NULL;
    }
    return 1;
}
//滤波 中值+均值，滤波窗口和次数可调节
//传入原始数据数组地址、原始数据长度、滤波滑动窗口大小以及滤波后数据
//数组地址，用于进行曲线平滑滤波。先进行中值滤波，后进行均值滤波，通过
//med_times、avg_times 控制滤波次数，通过 med_window_size、avg_window_size
//控制滤波窗口大小。使用该功能时，先建立长度为 data_length 的 output_data
//数组用于接收数据。常用的测试中，窗长扩增曲线可取 5-7，熔解曲线可取 21-
//31，滤波次数可为中值 1 次，均值 3 次
int filter(double* input_data, int data_length, int med_window_size, int avg_window_size, int med_times, int avg_times, double* output_data)
{
    double* temp_output = (double*)malloc(data_length * sizeof(double));
    if (temp_output == NULL) {LOGE("filter Memory allocation failed.\n");return -1; } // 检验非空
    memcpy(output_data, input_data, data_length * sizeof(double));
    for (int m = 0;m < med_times;m++)
    {
        median_filter(output_data, data_length, med_window_size, temp_output);
        memcpy(output_data, temp_output, data_length * sizeof(double));
    }
    for (int a = 0;a < avg_times;a++)
    {
        average_filter(output_data, data_length, avg_window_size, temp_output);
        memcpy(output_data, temp_output, data_length * sizeof(double));
    }
    free(temp_output);
    temp_output = NULL;
    return 1;
}




//重复值预处理，简单的对x数据进行中值匹配（不改变原数组的长度），0.01,0.01,0.03会变成0.01,0.02,0.03
int deduplication(double* x_raw_data, int raw_length)
{
    for (int i = 0;i < raw_length - 1;i++)
    {
        int count = 1;
        double bihind = *(x_raw_data + i);
        while (*(x_raw_data + i) == *(x_raw_data + i + count))
        {
            count++;
            if (count + i < raw_length)
                bihind = *(x_raw_data + i + count);
            else
            {
                bihind = *(x_raw_data + i) + 0.1;
                break;
            }
        }
        double increment = (bihind - *(x_raw_data + i)) / count;
        while (count != 1)
        {
            *(x_raw_data + i + count - 1) = *(x_raw_data + i) + increment * (count - 1);
            count--;
        }
    }
    return 0;
}


//重采样计算
int start_value(double* x_raw_data, double precision)
{
    int start = *x_raw_data / precision + 1;//起始数值计算
    return start;
}


int resampling(double* x_raw_data, int raw_length, double precision)
{
    int start = start_value(x_raw_data, precision);
    int end = *(x_raw_data + raw_length - 1) / precision;
    int knots = end - start + 1;//knots就是插值后的数组长度
    return knots;
}



//
//线性插值
//在这之前已经算出knots的大小
int interpolation(double* x_raw_data, double* y_raw_data, int raw_length, double* x_interp_data, double* y_interp_data, double precision)
{
    int start = start_value(x_raw_data, precision);
    int knots = resampling(x_raw_data, raw_length, precision);
    for (int i = 0;i < knots;i++)
        *(x_interp_data + i) = (double)(start + i) * precision;
    //线性插值
    for (int i = 0;i < knots;i++)
    {
        for (int j = 0;j < raw_length - 1;j++)
        {
            if (*(x_raw_data + j) < *(x_raw_data + j + 1) && *(x_raw_data + j) <= *(x_interp_data + i) && *(x_interp_data + i) <= *(x_raw_data + j + 1))
            {
                *(y_interp_data + i) = *(y_raw_data + j) + (*(x_interp_data + i) - *(x_raw_data + j)) * (*(y_raw_data + j + 1) - *(y_raw_data + j)) / (*(x_raw_data + j + 1) - *(x_raw_data + j));
                break;
            }
        }
    }
    return 1;
}



//扩增处理
//基线值计算，取选定区间内的荧光强度均值
double find_baseline(double* y_data, int left, int right)
{
    double mean = 0;
    for (int i = 0;i < right - left + 1;i++)
    {
        mean += *(y_data + left - 1 + i);
    }
    mean = mean / (right - left + 1);
    return mean;
}


//自动阈值，取选定区间内荧光强度标准差的10倍
double auto_threshold(double* y_data, int left, int right)
{
    double mean = find_baseline(y_data, left, right);
    double var = 0;
    for (int i = 0;i < right - left + 1;i++)
    {
        var += pow(*(y_data + left - 1 + i) - mean, 2) / (right - left + 1);
    }
    double auto_thres = pow(var, 0.5) * 10;
    return auto_thres;
}

//Ct值计算，设定阈值水平线与扩增曲线的交点的x坐标，精确度由precision调节，采用线性插值
double find_ct(double* x_data, double* y_data, int data_length, double threshold, double precision)
{
    deduplication(x_data, data_length);
    int interp_length = resampling(x_data, data_length, precision);
    double ct = 0;
    double* x_interp = (double*)malloc(interp_length * sizeof(double));
    double* y_interp = (double*)malloc(interp_length * sizeof(double));
    if (x_interp == NULL || y_interp == NULL) { LOGE("find_ct Memory allocation failed.\n");return -1.0f; } // 检验非空
    interpolation(x_data, y_data, data_length, x_interp, y_interp, precision);
    for (int i = 0;i < interp_length - 1;i++)
    {
        if (*(y_interp + i) < threshold && threshold < *(y_interp + i + 1))
            ct = *(x_interp + i);
    }
    free(x_interp);
    free(y_interp);
    x_interp = NULL;
    y_interp = NULL;
    return ct;
}


//相对荧光强度计算，扩增数据减去基线值
void deltaRn(double* y_data, int data_length, double baseline, double* y_deltaRn)
{
    for (int i = 0;i < data_length;i++)
    {
        *(y_deltaRn + i) = *(y_data + i) - baseline;
    }
}


//线性归一化
void linear_normalize(double* y_deltaRn, int data_length, double baseline, double* y_normailize)
{
    for (int i = 0;i < data_length;i++)
    {
        *(y_normailize + i) = *(y_deltaRn + i) / baseline;
    }
}

//对数归一化
void log_normalize(double* y_deltaRn, int data_length, double baseline, double* y_normailize)
{
    for (int i = 0;i < data_length;i++)
    {
//        double value = *(y_deltaRn + i) / baseline + 1;
//
//        if(value < 0){
//            value = 0.0f;
//        }
//
//        *(y_normailize + i) = log(value + 1) / log(2);

        *(y_normailize + i) = log(*(y_deltaRn + i) / baseline + 1) / log(2);

        if(*(y_normailize + i) == NAN)
        {
            *(y_normailize + i) = 0.0f;
        }

        if(*(y_normailize + i) == INFINITY)
        {
            *(y_normailize + i) = 10000.0f;
        }

        if(*(y_normailize + i) == -INFINITY)
        {
            *(y_normailize + i) = -10000.0f;
        }

    }
}


//熔解处理
//中心差分，得到熔解导数曲线 没有除以步长；
void derivation(double* x_input_data, double* y_input_data, int data_length, double* derived_data)
{
    for (int i = 0;i < data_length;i++)
    {
        double temp = 0;
        if (i == 0)
            temp = (*(y_input_data + i) - *(y_input_data + i + 1)); /// (*(x_input_data + i + 1) - *(x_input_data + i));
        else if (i == data_length - 1)
            temp = (*(y_input_data + i - 1) - *(y_input_data + i)); /// (*(x_input_data + i) - *(x_input_data + i - 1));
        else//中心差分
            temp = (*(y_input_data + i - 1) - *(y_input_data + i + 1))/2; /// (*(x_input_data + i + 1) - *(x_input_data + i - 1));
        *(derived_data + i) = temp;
    }
}


//熔解处理
//新增跨步差分  没有除以步长；
void derivation_step(double* x_input_data, double* y_input_data, int data_length, double* derived_data, int step)
{
    if (step < 1 || step > data_length - 1)
        step = 1;
    for (int i = 0;i < data_length;i++)
    {
        double temp = 0;
        if (i < step)
        {
            if (i == 0)
                temp = (*(y_input_data + 0) - *(y_input_data + 1)) / (*(x_input_data + 1) - *(x_input_data + 0));
            else
                temp = (*(y_input_data + 0) - *(y_input_data + i * 2)) / (*(x_input_data + i * 2) - *(x_input_data + 0));
        }
        else if (i >= data_length - step)
        {
            if (i == data_length - 1)
                temp = (*(y_input_data + data_length - 2) - *(y_input_data + data_length - 1)) / (*(x_input_data + data_length - 1) - *(x_input_data + data_length - 2));
            else
            {
                int idx = 2 * i - data_length + 1;
                temp = (*(y_input_data + idx) - *(y_input_data + data_length - 1)) /(*(x_input_data + data_length - 1) - *(x_input_data + idx));
            }
        }
        else // 中心差分
            temp = (*(y_input_data + i - step) - *(y_input_data + i + step)) /(*(x_input_data + i + step) - *(x_input_data + i - step));
        *(derived_data + i) = temp;
    }

}



//计算峰值个数便于malloc创建数组
int peakcount(double* y_input_data, int data_length)
{
    int count = 0;
    for (int i = 1;i < data_length - 1;i++)
    {
        if (*(y_input_data + i) > *(y_input_data + i - 1) && *(y_input_data + i) > *(y_input_data + i + 1))
        {
            count++;
        }
    }
    return count;
}

//寻峰
void peakdetecter(double* x_input_data, double* y_input_data, int data_length, int* melting_index, double* Tm, double* height, double* relheight)
{
    int count = 0;
    for (int i = 1;i < data_length - 1;i++)
    {
        if (*(y_input_data + i) > *(y_input_data + i - 1) && *(y_input_data + i) > *(y_input_data + i + 1))
        {
            int left = i - 1;
            int current = i;
            int right = i + 1;
            double leftmin = *(y_input_data + left);
            double rightmin = *(y_input_data + right);
            double relative_height;
            while (left > 0 && *(y_input_data + left) < *(y_input_data + current))
            {
                leftmin = *(y_input_data + left);
                left--;
                current--;
            }
            current = i;
            while (right < (data_length - 1) && *(y_input_data + right) < *(y_input_data + current))
            {
                rightmin = *(y_input_data + right);
                right++;
                current++;
            }
            if (left != 0)
                left++;
            if (right != data_length - 1)
                right--;
            //相对峰高计算
            relative_height = *(y_input_data + i) - (leftmin + (i - left) * (rightmin - leftmin) / (right - left));
            *(melting_index + count) = i;
            *(Tm + count) = x_input_data[i];
            *(height + count) = y_input_data[i];
            *(relheight + count) = relative_height;
            count++;
        }
    }
}


//寻峰加入阈值
int peakdetecter_optimization(double* x_input_data, double* y_input_data, int data_length, int* melting_index, double* Tm, double* height, double* relheight,
             bool optimization_enable, double enhance_lower, double enhance_upper, double enhance_gain, double weaken, double weaken_gain, double slope_thres)
{
    int count = 0;
    for (int i = 1;i < data_length - 1;i++)
    {
        if (*(y_input_data + i) > *(y_input_data + i - 1) && *(y_input_data + i) > *(y_input_data + i + 1))
        {
            int left = i - 1;
            int current = i;
            int right = i + 1;
            double leftmin = *(y_input_data + left);
            double rightmin = *(y_input_data + right);
            double reflevel;
            double relative_height;
            while (left > 0 && *(y_input_data + left) < *(y_input_data + current))
            {
                leftmin = *(y_input_data + left);
                left--;
                current--;
            }
            current = i;
            while (right < (data_length - 1) && *(y_input_data + right) < *(y_input_data + current))
            {
                rightmin = *(y_input_data + right);
                right++;
                current++;
            }
            if (left != 0)
                left++;
            if (right != data_length - 1)
                right--;
            //相对峰高计算
            reflevel = leftmin + (i - left) * (rightmin - leftmin) / (right - left);
            relative_height = *(y_input_data + i) - reflevel;
            //峰型优化
            if (optimization_enable == 1)
            {
                double optimized_y;
                double gain_left, gain_right;
                //斜率为正值
                double slope_left = (y_input_data[i] - leftmin) / (i - left);
                double slope_right = (y_input_data[i] - rightmin) / (right - i);
                bool slope_judge = slope_left < slope_thres && slope_right < slope_thres;
                if (enhance_lower < relative_height && relative_height < enhance_upper && !slope_judge)//相对峰高在enhance范围内，增强
                {
                    optimized_y = (y_input_data[i] - reflevel) * enhance_gain + reflevel;
                    gain_left = (optimized_y - leftmin) / (y_input_data[i] - leftmin);
                    gain_right = (optimized_y - rightmin) / (y_input_data[i] - rightmin);
                    for (int k = left; k < i; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - leftmin) * gain_left + leftmin;
                    }
                    for (int k = i; k < right; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - rightmin) * gain_right + rightmin;
                    }
                }
                else if (relative_height < weaken || slope_judge)//相对峰高小于weaken，削弱
                {
                    optimized_y = (y_input_data[i] - reflevel) * weaken_gain + reflevel;
                    gain_left = (optimized_y - leftmin) / (y_input_data[i] - leftmin);
                    gain_right = (optimized_y - rightmin) / (y_input_data[i] - rightmin);
                    for (int k = left; k < i; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - leftmin) * gain_left + leftmin;
                    }
                    for (int k = i; k < right; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - rightmin) * gain_right + rightmin;
                    }
                }
                //更新相对峰高
                relative_height = *(y_input_data + i) - (leftmin + (i - left) * (rightmin - leftmin) / (right - left));
            }
            *(melting_index + count) = i;
            *(Tm + count) = x_input_data[i];
            *(height + count) = y_input_data[i];
            *(relheight + count) = relative_height;
            count++;
        }
    }
    return count;
}




//寻峰
//2024/10/28添加峰型优化
//2025/03/27添加峰宽限制
void peakdetecter_optimization_limitwidth(double* x_input_data, double* y_input_data, int data_length, int* melting_index, double* Tm, double* height, double* relheight,
                                                  bool optimization_enable, double enhance_lower, double enhance_upper, double enhance_gain, double weaken, double weaken_gain, double slope_thres, int peak_width)
{
    int count = 0;
    double precision = *(x_input_data + 1) - *(x_input_data + 0);
    int peak_limit_side = (double)peak_width / 2.0 / precision;//单侧长度限制
    for (int i = 1;i < data_length - 1;i++)
    {
        if (*(y_input_data + i) > *(y_input_data + i - 1) && *(y_input_data + i) > *(y_input_data + i + 1))
        {
            int left = i - 1;
            int current = i;
            int right = i + 1;
            double leftmin = *(y_input_data + left);
            double rightmin = *(y_input_data + right);
            double reflevel;
            double relative_height;
            while (left > 0 && *(y_input_data + left) < *(y_input_data + current) && i - left < peak_limit_side)
            {
                leftmin = *(y_input_data + left);
                left--;
                current--;
            }
            current = i;
            while (right < (data_length - 1) && *(y_input_data + right) < *(y_input_data + current) && right - i < peak_limit_side)
            {
                rightmin = *(y_input_data + right);
                right++;
                current++;
            }
            if (left != 0)
                left++;
            if (right != data_length - 1)
                right--;
            //相对峰高计算
            reflevel = leftmin + (i - left) * (rightmin - leftmin) / (right - left);
            relative_height = *(y_input_data + i) - reflevel;
            //峰型优化
            if (optimization_enable == 1)
            {
                double optimized_y;
                double gain_left, gain_right;
                //斜率为正值
                double slope_left = (y_input_data[i] - leftmin) / (i - left);
                double slope_right = (y_input_data[i] - rightmin) / (right - i);
                bool slope_judge = slope_left < slope_thres && slope_right < slope_thres;
                if (enhance_lower < relative_height && relative_height < enhance_upper && !slope_judge)//相对峰高在enhance范围内，增强
                {
                    optimized_y = (y_input_data[i] - reflevel) * enhance_gain + reflevel;
                    gain_left = (optimized_y - leftmin) / (y_input_data[i] - leftmin);
                    gain_right = (optimized_y - rightmin) / (y_input_data[i] - rightmin);
                    for (int k = left; k < i; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - leftmin) * gain_left + leftmin;
                    }
                    for (int k = i; k < right; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - rightmin) * gain_right + rightmin;
                    }
                }
                else if (relative_height < weaken || slope_judge)//相对峰高小于weaken，削弱
                {
                    optimized_y = (y_input_data[i] - reflevel) * weaken_gain + reflevel;
                    gain_left = (optimized_y - leftmin) / (y_input_data[i] - leftmin);
                    gain_right = (optimized_y - rightmin) / (y_input_data[i] - rightmin);
                    for (int k = left; k < i; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - leftmin) * gain_left + leftmin;
                    }
                    for (int k = i; k < right; k++)
                    {
                        y_input_data[k] = (y_input_data[k] - rightmin) * gain_right + rightmin;
                    }
                }
                //更新相对峰高
                relative_height = *(y_input_data + i) - (leftmin + (i - left) * (rightmin - leftmin) / (right - left));
            }
            *(melting_index + count) = i;
            *(Tm + count) = x_input_data[i];
            *(height + count) = y_input_data[i];
            *(relheight + count) = relative_height;
            count++;
        }
    }
}

///////////Loess
//2024/10/28添加Loess滤波
// LoessSmoother的构造函数
LoessSmoother* create_loess_smoother(double* data, int dataCount, int width, int jump)
{
    LoessSmoother* smoother = (LoessSmoother*)malloc(sizeof(LoessSmoother));
    smoother->data = data;
    smoother->dataCount = dataCount;  // 初始化数据的个数
    smoother->smoothed = (double*)malloc(sizeof(double) * dataCount);
    smoother->width = width;
    smoother->jump = jump;
    smoother->interpolator = create_loess_interpolator(data, dataCount, width);
    smoother->weights = smoother->interpolator->weights;
    return smoother;
}

// LoessSmoother的析构函数
void destroy_loess_smoother(LoessSmoother* smoother)
{
    free(smoother->smoothed);
    smoother->smoothed = NULL;
    destroy_loess_interpolator(smoother->interpolator);
    free(smoother);
    smoother = NULL;
}

//LoessInterpolator构造
LoessInterpolator* create_loess_interpolator(double* data, int dataCount, int width)
{
    // 分配LoessInterpolator结构体的内存
    LoessInterpolator* interpolator = (LoessInterpolator*)malloc(sizeof(LoessInterpolator));
    if (interpolator == NULL)
    {
        // 处理内存分配失败
        return NULL;
    }

    // 初始化结构体的字段
    interpolator->data = data;
    interpolator->dataCount = dataCount;
    interpolator->width = width;

    // 为权重数组分配内存
    interpolator->weights = (double*)calloc(dataCount, sizeof(double));
    if (interpolator->weights == NULL)
    {
        // 处理内存分配失败
        free(interpolator);
        interpolator = NULL;
        return NULL;
    }

    return interpolator;
}
//析构
void destroy_loess_interpolator(LoessInterpolator* interpolator)
{
    if (interpolator == NULL)
    {
        return;
    }
    free(interpolator->weights);
    interpolator->weights = NULL;
    free(interpolator);
    interpolator = NULL;
}

// LoessSmoother的平滑方法调用
void loess_smoother_smooth(LoessSmoother* smoother, double* filtedData)
{
    int dataCount = smoother->dataCount;
    if (dataCount == 1)
    {
        smoother->smoothed[0] = smoother->data[0];
    }
    else
    {
        int left = -1;
        int right = -1;

        if (smoother->width >= dataCount)
        {
            left = 0;
            right = dataCount - 1;
            for (int i = 0; i < dataCount; i += smoother->jump)
            {
                double y = loess_interpolator_smooth(smoother->interpolator, i, left, right);
                smoother->smoothed[i] = y;
            }
        }
        else if (smoother->jump == 1)
        {//正常情况下会跑到这里
            int halfWidth = (smoother->width + 1) / 2;
            left = 0;
            right = smoother->width - 1;
            for (int i = 0; i < dataCount; ++i)
            {
                if ((i >= halfWidth) && (right != dataCount - 1))
                {
                    ++left;
                    ++right;
                }
                double y = loess_interpolator_smooth(smoother->interpolator, i, left, right);
                smoother->smoothed[i] = y;
            }
        }
        else
        {
            int halfWidth = (smoother->width + 1) / 2;
            for (int i = 0; i < dataCount; i += smoother->jump)
            {
                if (i < halfWidth - 1)
                {
                    left = 0;
                }
                else if (i >= dataCount - halfWidth)
                {
                    left = dataCount - smoother->width;
                }
                else
                {
                    left = i - halfWidth + 1;
                }
                right = left + smoother->width - 1;
                double y = loess_interpolator_smooth(smoother->interpolator, i, left, right);
                smoother->smoothed[i] = y;
            }

            if (smoother->jump != 1)
            {
                for (int i = 0; i < dataCount - smoother->jump; i += smoother->jump)
                {
                    double slope = (smoother->smoothed[i + smoother->jump] - smoother->smoothed[i]) / (double)smoother->jump;
                    for (int j = i + 1; j < i + smoother->jump; ++j)
                    {
                        smoother->smoothed[j] = smoother->smoothed[i] + slope * (j - i);
                    }
                }

                int last = dataCount - 1;
                int lastSmoothedPos = (last / smoother->jump) * smoother->jump;
                if (lastSmoothedPos != last)
                {
                    double y = loess_interpolator_smooth(smoother->interpolator, last, left, right);
                    smoother->smoothed[last] = y;
                    if (lastSmoothedPos != last - 1)
                    {
                        double slope = (smoother->smoothed[last] - smoother->smoothed[lastSmoothedPos]) / (last - lastSmoothedPos);
                        for (int j = lastSmoothedPos + 1; j < last; ++j)
                        {
                            smoother->smoothed[j] = smoother->smoothed[lastSmoothedPos] + slope * (j - lastSmoothedPos);
                        }
                    }
                }
            }
        }
    }
    for (int j = 0;j < dataCount;j++)
    {
        *(filtedData + j) = smoother->smoothed[j];
    }
}

//插值的实现：权重函数*数值
double loess_interpolator_smooth(LoessInterpolator* interpolator, int index, int left, int right)
{
    compute_neighbourhood_weights(interpolator, index, left, right);//传入interpolator是为了给weights赋值
    update_weights(interpolator, index, left, right);

    double ys = 0.0;
    for (int i = left; i <= right; ++i)
    {
        ys += interpolator->weights[i] * interpolator->data[i];
    }

    return ys;
}

// 三次方权重函数的实现
double tricube(double x)
{
    double a = fabs(x);
    if (a < 1.0)
    {
        return pow(1.0 - a * a * a, 3.0);
    }
    return 0.0;
}
// 计算邻域权重
void compute_neighbourhood_weights(LoessInterpolator* interpolator, double x, int left, int right)
{
    double lambda = fmax(x - left, right - x);

    if (interpolator->width > interpolator->dataCount)
    {
        lambda += (double)((interpolator->width - interpolator->dataCount) / 2);
    }

    double l999 = 0.999 * lambda;
    double l001 = 0.001 * lambda;

    double totalWeight = 0.0;
    for (int i = left; i <= right; ++i)
    {
        double delta = fabs(x - i);

        double weight = 0.0;
        if (delta <= l999)
        {
            if (delta <= l001)
            {
                weight = 1.0;
            }
            else
            {
                double fraction = delta / lambda;
                weight = tricube(fraction);
            }

            // 如果有外部权重，则应用它们
            // weight *= externalWeights[i]; // 这里需要外部权重的支持

            totalWeight += weight;
        }

        interpolator->weights[i] = weight;
    }

    // 归一化权重
    for (int i = left; i <= right; ++i)
    {
        interpolator->weights[i] /= totalWeight;
    }

}
//最小二乘迭代
void update_weights(LoessInterpolator* interpolator, double x, int left, int right)
{
    double x1Mean = 0.0;
    double x2Mean = 0.0;
    double x3Mean = 0.0;
    double x4Mean = 0.0;
    for (int i = left; i <= right; ++i)
    {
        double w = interpolator->weights[i];
        double x1w = i * w;
        double x2w = i * x1w;
        double x3w = i * x2w;
        double x4w = i * x3w;
        x1Mean += x1w;
        x2Mean += x2w;
        x3Mean += x3w;
        x4Mean += x4w;
    }

    double m2 = x2Mean - x1Mean * x1Mean;
    double m3 = x3Mean - x2Mean * x1Mean;
    double m4 = x4Mean - x2Mean * x2Mean;

    double denominator = m2 * m4 - m3 * m3;
    double range = interpolator->dataCount - 1;

    if (denominator > 0.000001 * range * range)
    {
        double beta2 = m4 / denominator;
        double beta3 = m3 / denominator;
        double beta4 = m2 / denominator;

        double x1 = x - x1Mean;
        double x2 = x * x - x2Mean;

        double a1 = beta2 * x1 - beta3 * x2;
        double a2 = beta4 * x2 - beta3 * x1;

        for (int i = left; i <= right; ++i)
        {
            interpolator->weights[i] *= (1 + a1 * (i - x1Mean) + a2 * (i * i - x2Mean));
        }
    }
}

//基线矫正：区间线性拟合（y = ax + b），并进行去趋势化
void baseline_detrend(double* x_data, double* y_data, int data_length, int left, int right, double* y_data_fit)
{
    // 下标从0开始
    left -= 1;
    right -= 1;

    if (left < 0 || right >= data_length || left >= right) {fprintf(stderr, "Invalid range for detrending.\n");return;}

    // 计算区间内 x, y 的均值
    double sum_x = 0.0, sum_y = 0.0, sum_xy = 0.0, sum_xx = 0.0;
    int n = right - left + 1;  // 区间内数据点数量

    for (int i = left; i <= right; i++)
    {
        sum_x += x_data[i];
        sum_y += y_data[i];
        sum_xy += x_data[i] * y_data[i];
        sum_xx += x_data[i] * x_data[i];
    }

    // 计算线性回归系数: y = ax + b
    double denominator = n * sum_xx - sum_x * sum_x;
    if (denominator == 0) {denominator = 0.0001;}

    double a = (n * sum_xy - sum_x * sum_y) / denominator;
    double b = (sum_y - a * sum_x) / n;

    // 计算去趋势化后的数据
    for (int i = 0; i < data_length; i++)
    {
        y_data_fit[i] = y_data[i] - (a * x_data[i]);  // 去趋势化
    }
}
