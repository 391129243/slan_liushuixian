package com.gidi.bio_pcr.fragment.system;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.PdfPageAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.Conclusion;
import com.gidi.bio_pcr.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HelpFragment extends BaseFragment {

    private List<Bitmap> pdfList;
    private PdfPageAdapter pageAdapter;
    private PdfRenderer pdfRenderer;
    private RecyclerView contentRv;
    private int pageCount;
    private int quality = 3;
    private RenderTask renderTask;
    private boolean hasRenderFinish= false;
    private ParcelFileDescriptor parcelFileDescriptor;
    //private PdfRenderer.Page curPdfPage;
    private String pdfLocalPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator+"PCR/file"+File.separator
            + "20231122.pdf";
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_help;
    }

    @Override
    protected void initView(View rootView) {
        contentRv = (RecyclerView) rootView.findViewById(R.id.content_rv);
        pageAdapter = new PdfPageAdapter(getActivity().getApplicationContext());
        contentRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL,
                false){
            @Override
            public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
                //设置半屏展示效果
                lp.width = getWidth();
                lp.height = getHeight()*2;
                return true;
            }
        });
        pageAdapter.setPdfList(pdfList);
        contentRv.setAdapter(pageAdapter);
        openPdf();
    }

    @Override
    protected void initListener() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pdfList = new ArrayList<Bitmap>();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
    }



    private void openPdf() {
        renderTask = new RenderTask();
        renderTask.execute();
    }

    private void releaseTask(){
        if(renderTask != null){
            renderTask.cancel(true);
            renderTask = null;
        }
    }

    private void release(){

        releaseTask();
        if(null!=pdfList){
            pdfList.clear();
            pdfList = null;
        }

        if (null != parcelFileDescriptor) {
            try {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (hasRenderFinish && pdfRenderer != null) {
            try {
                pdfRenderer.close();
                pdfRenderer = null;
            }catch (Exception e){
                e.getMessage();
                pdfRenderer.close();
                pdfRenderer = null;
            }

        }
  }

    public class RenderTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                //currentIndex = 0;
                pdfRenderer = new PdfRenderer(getFileDescriptor());
                pageCount = pdfRenderer.getPageCount();
                pdfList.clear();
                for (int i=0; i<pageCount; i++) {
                    PdfRenderer.Page item = pdfRenderer.openPage(i);
                    int qualityRatio = getResources().getDisplayMetrics().densityDpi / (quality * 72);
                    Bitmap bitmap = Bitmap.createBitmap(item.getWidth() ,item.getHeight(),
                            Bitmap.Config.ARGB_4444);
                    item.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    item.close();
                    pdfList.add(bitmap);
                }
                hasRenderFinish = true;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            //scrollSlider.setVisibility(pageCount == 0 ? GONE : VISIBLE);
            if (result) {
                pageAdapter.setPdfList(pdfList);
                pageAdapter.notifyDataSetChanged();
                contentRv.setVisibility(View.VISIBLE);
              //  loadingLayout.showContent();
            } else {
                //loadingLayout.showFail();
            }
            releaseTask();
        }
    }

    private ParcelFileDescriptor getFileDescriptor() {
        try {
            File file;
            if (pdfLocalPath.contains("asset")) {
                file = FileUtils.writeAssetsToFile(getContext(), pdfLocalPath);
            } else {
                file = new File(pdfLocalPath);
            }
            Log.i("AAA","pdfLocalPath" + pdfLocalPath);
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parcelFileDescriptor;
    }
}
