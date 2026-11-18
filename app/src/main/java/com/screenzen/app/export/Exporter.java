package com.screenzen.app.export;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.screenzen.app.R;
import com.screenzen.app.data.DbHelper;
import com.screenzen.app.data.EventDao;
import com.screenzen.app.data.model.OpenEvent;
import com.screenzen.app.util.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 导出到应用专属外部目录（getExternalFilesDir "exports"），再用分享面板发出去。 */
public final class Exporter {

    public static final String MIME_CSV = "text/csv";
    public static final String MIME_DB = "application/octet-stream";

    private Exporter() {
    }

    private static File exportDir(Context context) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建导出目录");
        }
        return dir;
    }

    public static File exportCsv(Context context) throws IOException {
        List<OpenEvent> events = new EventDao(context).listAll();
        File out = new File(exportDir(context), "breath_log_" + TimeUtils.fileStamp() + ".csv");

        try (OutputStream raw = new FileOutputStream(out);
             Writer writer = new OutputStreamWriter(raw, StandardCharsets.UTF_8)) {
            // BOM 直接写字节，不写成字符字面量 —— 免得再依赖源码文件本身的编码。
            // 不带 BOM 的话 Excel 会按 GBK 解，中文全乱。
            raw.write(0xEF);
            raw.write(0xBB);
            raw.write(0xBF);

            writer.write("时间,App名,包名,结果\r\n");
            for (OpenEvent event : events) {
                writer.write(csvField(TimeUtils.formatDateTime(event.createdAt)));
                writer.write(',');
                writer.write(csvField(event.appLabel));
                writer.write(',');
                writer.write(csvField(event.pkg));
                writer.write(',');
                writer.write(csvField(event.resultLabel()));
                writer.write("\r\n");
            }
        }
        return out;
    }

    public static File exportDatabase(Context context) throws IOException {
        File out = new File(exportDir(context), "screenzen_" + TimeUtils.fileStamp() + ".db");
        SQLiteDatabase db = DbHelper.get(context).getWritableDatabase();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // VACUUM INTO 产出一个完整单文件快照，不用再去管 -wal / -shm 边车文件
            String path = out.getAbsolutePath().replace("'", "''");
            db.execSQL("VACUUM INTO '" + path + "'");
        } else {
            // 低版本没有 VACUUM INTO：先把 WAL 落盘，再拷主文件
            db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)");
            if (out.exists() && !out.delete()) {
                throw new IOException("无法覆盖已存在的导出文件");
            }
            copy(new File(db.getPath()), out);
        }
        return out;
    }

    public static void share(Context context, File file, String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                context, context.getPackageName() + ".fileprovider", file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mimeType);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, context.getString(R.string.common_share));
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    private static String csvField(String raw) {
        if (raw == null) {
            return "";
        }
        String escaped = raw.replace("\"", "\"\"");
        boolean needsQuote = escaped.indexOf(',') >= 0
                || escaped.indexOf('"') >= 0
                || escaped.indexOf('\n') >= 0
                || escaped.indexOf('\r') >= 0;
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static void copy(File source, File target) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }
}
