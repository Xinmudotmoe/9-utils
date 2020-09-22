package moe.xinmu.pagehelper.test.mysql;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class SystemBlacklistWord {
    private long id;
    private String text;
    private int isPublish;
    private int isDelete;
    private LocalDateTime createTime;
    private Date updateTime;

}
