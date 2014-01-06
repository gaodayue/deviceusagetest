package bugbuster.deviceusage.test;

import java.io.Closeable;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;
import bugbuster.deviceusage.access.AppStatistics;
import bugbuster.deviceusage.access.SQLiteLocalStore;

public class SQLiteLocalStoreTest extends InstrumentationTestCase {

	private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "bugbusters_db";
    private static final String DATABASE_PATH = "/data/data/bugbuster.deviceusage/databases/";
    
    private static final String TABLE_NAME = "app_statistics";
    private static final String COLNAME_PKGNAME	= "pkgname";
    private static final String COLNAME_VERSION	= "version";
    private static final String COLNAME_FGTIME	= "fgtime";	// how long the app's activity is in foreground
    private static final String COLNAME_FGCOUNT	= "fgnum";	// how many times user starts the app's activity
    private static final String COLNAME_BGTIME	= "bgtime";	// how long the app runs services in background
    private static final String COLNAME_BGCOUNT	= "bgnum";	// how many times the app runs services in background
    private static final String COLNAME_SEND	= "sendbytes";		// total bytes send to network
    private static final String COLNAME_RECEIVE	= "receivebytes"; 	// total bytes receive from network
    private static final String COLNAME_DISK	= "diskusage";	// total bytes of app's data usage
        
    private SQLiteLocalStore db;
    //private RenamingDelegatingContext context;
    private Context context;
    
    public void setUp(){
    	//super.setUp();
    	//context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
    	context = getInstrumentation().getTargetContext();
    	db = new SQLiteLocalStore(context);
    	//db.startService()
    	//db.putAppStatistics(AppStatistics newApp);
    	//dn.stopService();
    }
    
    public void testPreCondition() {
    	assertNotNull(db);
    	db.startService();
    	verifyTableExists(TABLE_NAME);
    	db.stopService();
    }
    
    public void testPutApp(){
    	clearDB();
    	db.startService();
    	{
    		AppStatistics app = new AppStatistics("com.testApp.hello", "1.0.1",
    				0, 0, 0, 0, 0, 0, 0);
    		delayOneSecond();
    		db.putAppStatistics(app);
    	}
    	{
    		AppStatistics app = new AppStatistics("com.world.testApp", "2.0.2",
    				0, 0, 0, 0, 0, 0, 0);
    		delayOneSecond();
    		db.putAppStatistics(app);
    	}
    	{
    		AppStatistics app = new AppStatistics("com.testApp.hello", "1.0.1",
    				1, 1, 1, 1, 1, 1, 1);
    		delayOneSecond();
    		db.putAppStatistics(app);
    	}
    	verifyRowCount(2);		
    	db.stopService();
    }
    
    public void testReadApp() {
    	clearDB();
    	String randomName = "this.is.random.app";
    	db.startService();
    	{
    		AppStatistics app = new AppStatistics(randomName, "1.0",
    				0, 0, 0, 0, 0, 0, 0);
    		delayOneSecond();
    		db.putAppStatistics(app);
    	}
		verifyRowWithPackageName(randomName);
		verifyRowCount(1);

    	db.stopService();
    }
    
    public void testDeleteApp() {
    	clearDB();
    	String anotherRandomName = "another.random.app";
    	db.startService();
    	{
    		AppStatistics app = new AppStatistics(anotherRandomName, "1.1.1",
    				0, 0, 0, 0, 0, 0, 0);
    		delayOneSecond();
    		db.putAppStatistics(app);
    	}
		verifyRowDelete(anotherRandomName);

    	db.stopService();
    }
    
    void verifyRowDelete(String packageName) {
    	SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("delete from " + TABLE_NAME + " where " 
                    + COLNAME_PKGNAME + " like '" + packageName + "'" );
            
            c = db.rawQuery("select " + COLNAME_PKGNAME + " from " + TABLE_NAME + " where " + 
                    COLNAME_PKGNAME + " like '" + packageName + "' limit 1" ,
                    null);
            boolean isRowDeleted = false;
            if (c.getCount() > 0) {
            	isRowDeleted = false;
            }
            else {
            	isRowDeleted = true;
            }
            assertTrue(isRowDeleted);
        } finally {
            closeResource(c);
            closeResource(db);
        }
    }
    
    void verifyRowCount(int referenceRowCount) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("select count(*) from " + TABLE_NAME, null);
            if (c.getCount() == 1) {
                assertTrue(c.moveToFirst());
                assertEquals(c.getInt(0), referenceRowCount);
            }
        } finally {
            closeResource(c);
            closeResource(db);
        }
    }
    
    void verifyTableExists(String tableName) {
    	SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            c = db.query("'sqlite_master'", new String[] {
                    "name"
            }, "type='table'", null, null, null, null);

            boolean isTableFound = false;
            while(c.moveToNext()) {
                if(tableName.equals(c.getString(0))) {
                	isTableFound = true;
                    break;
                }
            }

            assertEquals(true, isTableFound);
        } finally {
            closeResource(c);
            closeResource(db);
        }
    }
    
    void verifyRowWithPackageName(String packageName) {
    	SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("select " + COLNAME_PKGNAME + " from " + TABLE_NAME + " where " + 
                    COLNAME_PKGNAME + " like '" + packageName + "' limit 1" ,
                    null);
            if (c.getCount() == 1) {
                c.moveToFirst();
                assertEquals(packageName, c.getString(0));
            } else {
                fail();
            }
        } finally {
            closeResource(c);
            closeResource(db);
        }
    }
    
    void verifyDatabaseVersion(int expectedVersion) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            assertEquals(expectedVersion, db.getVersion());
        } finally {
        	closeResource(db);
        }
    }
    
    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                fail("Can't close " + resource.toString());
            }
        }
    }
    
    private void delayOneSecond() {
    	long sleepTime = (1000) + 100;
        SystemClock.sleep(sleepTime);
    }
    
    void clearDB() {
    	//context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
    	context = getInstrumentation().getTargetContext();
    	if (!context.deleteDatabase(DATABASE_NAME)) {
    		Log.e("TESTCASE", "error deleting database");
    	}
    }
}
