
package com.sar.gp.dld;

import java.util.Map;

public interface DownloadTaskListener {

    public void updateProcess(Map<String, Object> data);

    public void finishDownload(Map<String, Object> data);

    public void preDownload(Map<String, Object> data);

    public void errorDownload(Map<String, Object> data);
}
