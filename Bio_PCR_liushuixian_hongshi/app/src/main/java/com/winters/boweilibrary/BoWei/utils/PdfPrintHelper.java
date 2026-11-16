package com.winters.boweilibrary.BoWei.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfPrintHelper {
    private Context context;
    private PrintAttributes printAttributes;
    private String fileName;

    public PdfPrintHelper(Context context) {
        this.context = context;
        // 默认打印属性设置
        this.printAttributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();
    }

    public void printPdf(final List<Bitmap> pages, String fileName) {
        this.fileName = fileName;

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
