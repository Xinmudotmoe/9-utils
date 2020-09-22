package moe.xinmu.pagehelper.test.mssql;

import lombok.Data;

@Data
public class Schemata {
    String catalogName;
    String schemaName;
    String schemaOwner;
    String defaultCharacterSetCatalog;
    String defaultCharacterSetSchema;
    String defaultCharacterSetName;
}
