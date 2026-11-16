package com.winters.boweilibrary.printer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfPrintHelper {

    private static final String TAG = "PdfPrintHelper";

    private static PrintAttributes printAttributes;

    public static void printDocument(String filename,Context context) {
        // 准备要打印的位图列表
        List<Bitmap> pages = new ArrayList<>();

        // 添加页面（这里只是示例，你需要根据实际情况创建位图）
        try {
            // 从assets加载PDF并转换为位图
            ParcelFileDescriptor input = ParcelFileDescriptor.open(new File(filename), ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(input);
            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                pages.add(bitmap);

                page.close();
            }
            pdfRenderer.close();

            if(printAttributes == null) {
                printAttributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
            }

            printPdf(pages, "打印文档.pdf",context);

        } catch (IOException e) {
            Log.e(TAG, "printDocument:" + e.getMessage());
        }
    }


    public static void printPdf(final List<Bitmap> pages, String fileName ,Context context) {

        PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

        PrintDocumentAdapter printAdapter = new PrintDocumentAdapter() {
            private PrintedPdfDocument pdfDocument;

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {

                // 创建PDF文档
                pdfDocument = new PrintedPdfDocument(context, newAttributes);

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                        .Builder(fileName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pages.size());

                PrintDocumentInfo info = builder.build();
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal, WriteResultCallback callback) {

                try {
                    // 遍历所有页面并写入PDF
                    for (int i = 0; i < pages.size(); i++) {
                        if (cancellationSignal.isCanceled()) {
                            callback.onWriteCancelled();
                            pdfDocument.close();
                            return;
                        }

                        // 开始一个新页面
                        PdfDocument.Page page = pdfDocument.startPage(i);

                        // 获取页面的Canvas
                        Canvas canvas = page.getCanvas();

                        // 计算缩放比例以适应页面
                        Bitmap bitmap = pages.get(i);
                        float scale = Math.min(
                                (float) canvas.getWidth() / bitmap.getWidth(),
                                (float) canvas.getHeight() / bitmap.getHeight()
                        );

                        // 计算居中位置
                        float left = (canvas.getWidth() - bitmap.getWidth() * scale) / 2;
                        float top = (canvas.getHeight() - bitmap.getHeight() * scale) / 2;

                        // 创建变换矩阵
                        Matrix matrix = new Matrix();
                        matrix.setScale(scale, scale);
                        matrix.postTranslate(left, top);

                        // 绘制位图
                        canvas.drawBitmap(bitmap, matrix, null);

                        // 完成页面
                        pdfDocument.finishPage(page);
                    }

                    // 将PDF写入目标文件
                    pdfDocument.writeTo(new FileOutputStream(
                            destination.getFileDescriptor()));

                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                } finally {
                    pdfDocument.close();
                }
            }
        };


        // 开始打印任务
        printManager.print(fileName, printAdapter, printAttributes);
    }

    // 设置自定义打印属性
    public void setPrintAttributes(PrintAttributes attributes) {
        this.printAttributes = attributes;
    }
}
