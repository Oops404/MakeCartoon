package personal.chenhsichin.make.cartoon;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.io.File;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * @author ChenHsiChin cheneyjin@outlook.com
 * @date 2018-09-11
 * @bilibili 翻滚吧年糕君 ID：1489684
 */
public class Cartoon {

    private String input, output;
    private File[] files;
    private NumberFormat numberFormat = NumberFormat.getNumberInstance();

    static {
        System.load(getRoot() + "/libs/x64/opencv_java343.dll");
    }

    private static String getRoot() {
        return new File("").getAbsolutePath();
    }

    public Cartoon(String input, String output) throws Exception {
        this.input = input;
        this.output = output;
        files = new File(input).listFiles();
        if (files == null || files.length == 0) {
            throw new Exception("folder is empty");
        }
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setRoundingMode(RoundingMode.UP);
    }

    public void draw() {
        Mat temp = new Mat();
        Mat edge = new Mat();
        int index = 0;
        for (File file : files) {
            long start = System.currentTimeMillis();
            // 因为我考虑批量处理很多图片，所以判断下输出目录
            // 是不是已经存在了处理好的图片，避免重复处理
            boolean exist = new File(output + file.getName()).exists();
            if (exist || file.isDirectory()) {
                continue;
            }
            Mat ori = Imgcodecs.imread(file.getAbsolutePath());
            // 这里我们拷贝一份原图，用于线条勾勒
            Mat fnmdcMat = ori.clone();
            /*
              这里我们对源图片进行双线滤波，将图片中物体色块涂抹开，
              让图片变的更像水粉涂抹的样子
              一共对图片迭代六次滤波
             */
            for (int i = 0; i < 6; i++) {
                Imgproc.bilateralFilter(ori, temp, 17, 17, 13);
                // 讲单次滤波结果拷贝回Ori原图
                temp.copyTo(ori);
            }
            /*
               这里的Photo.fastNlMeansDenoisingColored函数用于对彩图进行椒盐去噪
               避免我们做的水彩画出现大量密集的线条勾勒，画面失真
               参数说明:
               h 过滤强度
               hColor 同上，用于彩色图像的过滤强度
               templateWindowSize 奇数 建议7
               searchWindowSize 奇数 建议21
             */
            Photo.fastNlMeansDenoisingColored(fnmdcMat, fnmdcMat,
                    8, 8, 7, 21);
            /**
             * 然后我们对fnmdcMat进行画线，就像铅笔画画线一样
             * 这里我们用Canny算法进行边缘检测
             */
            Imgproc.Canny(fnmdcMat, edge, 80, 210);
            // canny图是灰度图，线条为白色，因此这里我们做取反操作变为黑线
            Core.bitwise_not(edge, edge);
            // 将灰度图重新恢复到三通道图
            Imgproc.cvtColor(edge, edge, Imgproc.COLOR_GRAY2BGR);
            // TODO 接下来其实可以将canny图勾勒出来的线条进行处理，变的更像铅笔线条的效果，
            // TODO 但是暂时效果不是很完美，就不写上来
            // 将线条和双线滤波后的图像叠加
            Core.bitwise_and(ori, edge, ori);
            // TODO 接下来我们还可以做一下resize 进行插值抗锯齿，但是我发现插值后的结果没那么好看，遂罢
            Imgcodecs.imwrite(output + file.getName(), ori);
            // 释放mat资源
            ori.release();
            fnmdcMat.release();
            System.out.println(numberFormat.format(((float) (++index) / (float) (files.length)) * 100)
                    + "% " + file.getName() + " spend: " + (System.currentTimeMillis() - start) / 1000 + "S");
        }
        temp.release();
        edge.release();
    }

    public static void main(String[] args) throws Exception {
        new Cartoon("F:\\sakura\\", "F:\\sakura_out\\").draw();
    }
}
