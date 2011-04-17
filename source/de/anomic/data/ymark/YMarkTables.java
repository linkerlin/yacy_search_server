// YMarkTables.java
// (C) 2011 by Stefan Förster, sof@gmx.de, Norderstedt, Germany
// first published 2010 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.anomic.data.ymark;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.anomic.data.WorkTables;

import net.yacy.kelondro.blob.Tables;
import net.yacy.kelondro.blob.Tables.Data;
import net.yacy.kelondro.index.RowSpaceExceededException;

public class YMarkTables {
    
    public static enum TABLES {
		BOOKMARKS ("_bookmarks"),
		TAGS ("_tags"),
		FOLDERS ("_folders");
		
		private String basename;
		
		private TABLES(String b) {
			this.basename = b;
		}
		public String basename() {
			return this.basename;
		}
		public String tablename(String bmk_user) {
			return bmk_user+this.basename;
		}
	}
	
	public static enum PROTOCOLS {
    	HTTP ("http://"),
    	HTTPS ("https://");
    	
    	private String protocol;
    	
    	private PROTOCOLS(String s) {
    		this.protocol = s;
    	}
    	public String protocol() {
    		return this.protocol;
    	}
    	public String protocol(String s) {
    		return this.protocol+s;
    	}
    }
		
    public static enum BOOKMARK {
    	//             key                 dflt            html_attrb          xbel_attrb      json_attrb      type
    	URL            ("url",             "",             "href",             "href",         "uri",          "link"),
    	TITLE          ("title",           "",             "",                 "",             "title",        "meta"),
    	DESC           ("desc",            "",             "",                 "",             "",             "comment"),
    	DATE_ADDED     ("date_added",      "",             "add_date",         "added",        "dateAdded",    "date"),
    	DATE_MODIFIED  ("date_modified",   "",             "last_modified",    "modified",     "lastModified", "date"),
    	DATE_VISITED   ("date_visited",    "",             "last_visited",     "visited",      "",             "date"),
    	PUBLIC         ("public",          "flase",        "",                 "yacy:public",  "",             "lock"),
    	TAGS           ("tags",            "unsorted",     "shortcuturl",      "yacy:tags",    "keyword",      "tag"),
    	VISITS         ("visits",          "0",            "",                 "yacy:visits",  "",             "stat"),
    	FOLDERS        ("folders",         "/unsorted",    "",                 "",             "",             "folder");
    	    	
    	private String key;
    	private String dflt;
    	private String html_attrb;
    	private String xbel_attrb;
    	private String json_attrb;
    	private String type;

        private static final Map<String,BOOKMARK> lookup = new HashMap<String,BOOKMARK>();
        static {
        	for(BOOKMARK b : EnumSet.allOf(BOOKMARK.class))
        		lookup.put(b.key(), b);
        }
    	
        private static StringBuilder buffer = new StringBuilder(25);;
        
    	private BOOKMARK(String k, String s, String a, String x, String j, String t) {
    		this.key = k;
    		this.dflt = s;
    		this.html_attrb = a;
    		this.xbel_attrb = x;
    		this.json_attrb = j;
    		this.type = t;
    	}
    	public static BOOKMARK get(String key) { 
            return lookup.get(key); 
    	}
    	public static boolean contains(String key) {
    		return lookup.containsKey(key);
    	}
    	public String key() {
    		return this.key;
    	}    	
    	public String deflt() {
    		return  this.dflt;
    	}
    	public String html_attrb() {
    		return this.html_attrb;
    	}
    	public String xbel_attrb() {
    		return this.xbel_attrb;
    	}
        public String json_attrb() {
            return this.json_attrb;
        }
    	public String xbel() {
    		buffer.setLength(0);
    		buffer.append('"');
    		buffer.append('\n');
    		buffer.append(' ');
    		buffer.append(this.xbel_attrb);
    		buffer.append('=');
    		buffer.append('"');
    		return buffer.toString();
    	}
    	public String type() {
    		return this.type;
    	}
    }
    
    public final static HashMap<String,String> POISON = new HashMap<String,String>();

    public final static String FOLDERS_ROOT = "/"; 
    public final static String FOLDERS_UNSORTED = "/unsorted";
    public final static String FOLDERS_IMPORTED = "/imported";
	public static final int FOLDER_BUFFER_SIZE = 100;    
    
    public final static String BOOKMARKS_LOG = "BOOKMARKS";
    public final static String BOOKMARKS_ID = "id";
	
    public final static String USER_ADMIN = "admin";
	public final static String USER_AUTHENTICATE = "AUTHENTICATE";
	public final static String USER_AUTHENTICATE_MSG = "Authentication required!";
	
    public final static String p1 = "(?:^|.*,)";
    public final static String p2 = "\\Q";
    public final static String p3 = "\\E";
    public final static String p4 = "(?:,.*|$)";
    public final static String p5 = "((?:";
    public final static String p6 = "),*.*){";
    public final static String p7 = "/.*)";
    public final static String p8 = "(?:,|$)";
	
    private final WorkTables worktables;
    private final StringBuffer patternBuilder;
    
    public YMarkTables(final Tables wt) {
    	this.worktables = (WorkTables)wt;
    	this.patternBuilder = new StringBuffer(512);
    }
   
    public void deleteBookmark(final String bmk_user, final byte[] urlHash) throws IOException, RowSpaceExceededException {
        final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
    	Tables.Row bmk_row = null;
        bmk_row = this.worktables.select(bmk_table, urlHash);
        if(bmk_row != null) {
    		this.worktables.delete(bmk_table,urlHash);
        }
    }
    
    public void deleteBookmark(final String bmk_user, final String url) throws IOException, RowSpaceExceededException {
    	this.deleteBookmark(bmk_user, YMarkUtil.getBookmarkId(url));
    }
    
    public TreeSet<String> getFolders(final String bmk_user, final String root) throws IOException {
    	final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
    	this.patternBuilder.setLength(0);
    	this.patternBuilder.append(p1);
    	this.patternBuilder.append('(');
    	this.patternBuilder.append(root);
    	this.patternBuilder.append(p7);
    	this.patternBuilder.append(p8);
    	
    	final Pattern r = Pattern.compile(this.patternBuilder.toString());
    	final Iterator<Tables.Row> bit = this.worktables.iterator(bmk_table, YMarkTables.BOOKMARK.FOLDERS.key(), r);
    	final TreeSet<String> folders = new TreeSet<String>();
    	final StringBuilder path = new StringBuilder(200);
    	Tables.Row bmk_row = null;
    	
    	while(bit.hasNext()) {
    		bmk_row = bit.next();
    		if(bmk_row.containsKey(BOOKMARK.FOLDERS.key())) {    	    	
    			final String[] folderArray = (new String(bmk_row.get(BOOKMARK.FOLDERS.key()),"UTF8")).split(YMarkUtil.TAGS_SEPARATOR);                    
    	        for (final String folder : folderArray) {
    	            if(folder.length() > root.length() && folder.substring(0, root.length()+1).equals(root+'/')) {
    	                if(!folders.contains(folder)) {
        	        		path.setLength(0);
        	                path.append(folder);
        	                //TODO: get rid of .toString.equals()
        	                while(path.length() > 0 && !path.toString().equals(root)){
        	                	folders.add(path.toString());                  
        	                	path.setLength(path.lastIndexOf(YMarkUtil.FOLDERS_SEPARATOR));
        	                }	
    	        		}
    	        	}    	        	
    	        }
    		}
    	}
        if (!root.equals(YMarkTables.FOLDERS_ROOT)) { folders.add(root); }    	
        return folders;
    }
    
    public Iterator<Tables.Row> getBookmarksByFolder(final String bmk_user, final String folder) throws IOException {    	
    	final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
    	this.patternBuilder.setLength(0);
    	this.patternBuilder.append(p1);
    	this.patternBuilder.append('(');
    	this.patternBuilder.append(p2);
		this.patternBuilder.append(folder);
		this.patternBuilder.append(p3);
		this.patternBuilder.append(')');
		this.patternBuilder.append(p4);
    	final Pattern p = Pattern.compile(this.patternBuilder.toString());
    	return this.worktables.iterator(bmk_table, YMarkTables.BOOKMARK.FOLDERS.key(), p);
    }
    
    public Iterator<Tables.Row> getBookmarksByTag(final String bmk_user, final String[] tagArray) throws IOException {    	
    	final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
        this.patternBuilder.setLength(0);
    	this.patternBuilder.append(p1);
    	this.patternBuilder.append(p5);
    	for (final String tag : tagArray) {
        	this.patternBuilder.append(p2);
    		this.patternBuilder.append(tag);
    		this.patternBuilder.append(p3);
        	this.patternBuilder.append('|');
		}
    	this.patternBuilder.deleteCharAt(this.patternBuilder.length()-1);
    	this.patternBuilder.append(p6);
    	this.patternBuilder.append(tagArray.length);
    	this.patternBuilder.append('}');
    	final Pattern p = Pattern.compile(this.patternBuilder.toString());
    	return this.worktables.iterator(bmk_table, YMarkTables.BOOKMARK.TAGS.key(), p);
    }
    
	public void addBookmark(final String bmk_user, final HashMap<String,String> bmk, final boolean importer) throws IOException, RowSpaceExceededException {
		final String bmk_table = TABLES.BOOKMARKS.tablename(bmk_user);
        final String date = String.valueOf(System.currentTimeMillis());
		final byte[] urlHash = YMarkUtil.getBookmarkId(bmk.get(BOOKMARK.URL.key()));
		Tables.Row bmk_row = null;

		if (urlHash != null) {
			bmk_row = this.worktables.select(bmk_table, urlHash);
	        if (bmk_row == null) {
	        	// create and insert new entry
	        	final Data data = new Data();
	            for (BOOKMARK b : BOOKMARK.values()) {
	            	switch(b) {
	    				case DATE_ADDED:
	    				case DATE_MODIFIED:
	    					if(bmk.containsKey(b.key()) && bmk.get(b.key()) != null) {
	    						data.put(b.key(), bmk.get(b.key()));
	    					} else {
	    						data.put(b.key(), String.valueOf(System.currentTimeMillis()).getBytes());
	    					}
	    					break;
	    				case TAGS:
	    					if(bmk.containsKey(b.key()) && bmk.get(b.key()) != null) {
	    						data.put(b.key(), bmk.get(b.key()));
	    					} else {
	    						data.put(b.key(), b.deflt());	
	    					}
	    					break;
	    				case FOLDERS:
	    					if(bmk.containsKey(b.key()) && bmk.get(b.key()) != null) {
	    						data.put(b.key(), bmk.get(b.key()));
	    					} else {
	    						data.put(b.key(), b.deflt());	
	    					}
	    					break;	
	    				default:
	    					if(bmk.containsKey(b.key()) && bmk.get(b.key()) != null) {
	    						data.put(b.key(), bmk.get(b.key()));
	    					}
	            	 }
	             }
            	 this.worktables.insert(bmk_table, urlHash, data);
	        } else {	
            	// modify and update existing entry
                HashSet<String> oldSet;
                HashSet<String> newSet;
	        	for (BOOKMARK b : BOOKMARK.values()) {
	            	switch(b) {
	    				case DATE_ADDED:
	    					if(!bmk_row.containsKey(b.key))
	    						bmk_row.put(b.key(), date); 
	    					break;
	    				case DATE_MODIFIED:
	    					bmk_row.put(b.key(), date); 
	    					break;
	    				case TAGS:
	    	            	oldSet = YMarkUtil.keysStringToSet(bmk_row.get(b.key(),b.deflt()));
	    	            	if(bmk.containsKey(b.key())) {
	    	            		newSet = YMarkUtil.keysStringToSet(bmk.get(b.key()));
	    	            		if(importer) {
		    	            		newSet.addAll(oldSet);
		    	            		bmk_row.put(b.key(), YMarkUtil.keySetToString(newSet));
		    	            		oldSet.clear();
	    	            		} else {
	    	            			bmk_row.put(b.key, bmk.get(b.key()));
	    	            		}
	    	            	} else {
	    	            		newSet = new HashSet<String>();
	    	            		bmk_row.put(b.key, bmk_row.get(b.key(), b.deflt()));
	    	            	}				
	    	            	break;
	    				case FOLDERS:
	    					oldSet = YMarkUtil.keysStringToSet(bmk_row.get(b.key(),b.deflt()));
	    					if(bmk.containsKey(b.key())) {
	    	            		newSet = YMarkUtil.keysStringToSet(bmk.get(b.key()));
	    	            		if(importer) {
		    	            		newSet.addAll(oldSet);
		    	            		bmk_row.put(b.key(), YMarkUtil.keySetToString(newSet));
		    	            		oldSet.clear();
	    	            		} else {
	    	            			bmk_row.put(b.key, bmk.get(b.key()));
	    	            		}
	    	            	} else {
	    	            		newSet = new HashSet<String>();
	    	            		bmk_row.put(b.key, bmk_row.get(b.key(), b.deflt()));
	    	            	}
	    					break;	
	    				default:
	    					if(bmk.containsKey(b.key())) {
	    						bmk_row.put(b.key, bmk.get(b.key()));
	    					} else {
	    						bmk_row.put(b.key, bmk_row.get(b.key(), b.deflt()));
	    					}
	            	 }
	             }
                // update bmk_table
                this.worktables.update(bmk_table, bmk_row); 
            }
		}
	}
}