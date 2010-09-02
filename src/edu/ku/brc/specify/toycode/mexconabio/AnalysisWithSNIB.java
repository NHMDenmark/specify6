/* Copyright (C) 2009, University of Kansas Center for Research
 * 
 * Specify Software Project, specify@ku.edu, Biodiversity Institute,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package edu.ku.brc.specify.toycode.mexconabio;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import edu.ku.brc.specify.conversion.BasicSQLUtils;

public class AnalysisWithSNIB extends AnalysisBase
{
    private static final Logger  log                = Logger.getLogger(AnalysisWithSNIB.class);
    
    /**
     * 
     */
    public AnalysisWithSNIB()
    {
        super();
    }

    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.toycode.mexconabio.AnalysisBase#process(int, int)
     */
    @Override
    public void process(final int typeArg, final int options)
    {
        processGBIF(DO_ALL);
    }

    
    /**
     * @param options
     */
    public void processConabio(final int options)
    {
        PreparedStatement[] pStmts    = new PreparedStatement[2];
        PreparedStatement[] pCntStmts = new PreparedStatement[2];
        
        try
        {
            String michWhere = " WHERE CollNr IS NOT NULL ORDER BY CollNr";
            String michSQL   = "SELECT BarCD, CollNr, GenusName, SpeciesName, SubspeciesName, Collectoragent1, LocalityName, Datecollstandrd, COUNTRY FROM conabio" + michWhere;

            String cntSQL = "SELECT COUNT(*) FROM conabio" + michWhere;
            log.debug(cntSQL);

            String snibSQL   = "SELECT CatalogNumber, CollectorNumber,  Genus, Species, Nameinfraspecies, LastNameFather, LastNameMother, FirstName, Locality, Latitude, Longitude, `Year`, `Month`, `Day`, Country ";
            
            // Query without Year
            String whereStr = " FROM angiospermas WHERE CollectorNumber = ? AND Genus = ?";
            String strSQL   = String.format("SELECT COUNT(*)" + whereStr);
            pCntStmts[0]    = dbGBIFConn.prepareStatement(strSQL + whereStr);
            
            strSQL       = String.format(snibSQL + whereStr);
            pStmts[0]    = dbGBIFConn.prepareStatement(strSQL);
            
            // Query with Year
            whereStr     = " FROM angiospermas WHERE CollectorNumber = ? AND Genus = ? AND YEAR = ?";
            strSQL       = String.format("SELECT COUNT(*)" + whereStr);
            pCntStmts[1] = dbGBIFConn.prepareStatement(strSQL + whereStr);
            
            strSQL = String.format(snibSQL + whereStr);
            pCntStmts[1] = dbGBIFConn.prepareStatement(strSQL);

            process("analysis", "conabio", cntSQL, michSQL, pCntStmts, pCntStmts, 3, false);
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally 
        {
            try
            {
                for (int i=0;i<pCntStmts.length;i++)
                {
                    pCntStmts[i].close();
                    pStmts[i].close();
                }
            } catch (Exception ex)
            {
                
            }
            System.out.println("Done.");
        }

    }

    /**
     * @param options
     */
    public void processGBIF(final int options)
    {
        PreparedStatement[] pStmts    = new PreparedStatement[2];
        PreparedStatement[] pCntStmts = new PreparedStatement[2];
        
        try
        {
            String michWhere = " WHERE CollNr IS NOT NULL ORDER BY CollNr";
            String michSQL   = "SELECT BarCD, CollNr, GenusName, SpeciesName, SubspeciesName, Collectoragent1, LocalityName, Datecollstandrd, COUNTRY FROM conabio" + michWhere;

            String cntSQL = "SELECT COUNT(*) FROM conabio" + michWhere;
            log.debug(cntSQL);

            String gbifSQL = "SELECT catalogue_number, collector_num, genus, species, subspecies, collector_name, locality, latitude, longitude, `year`, `month`, `day`, country";

            // Query without Year
            String whereStr = " FROM raw WHERE collector_num = ? AND genus = ?";
            pCntStmts[0]    = dbGBIFConn.prepareStatement("SELECT COUNT(*)" + whereStr);
            pStmts[0]       = dbGBIFConn.prepareStatement(gbifSQL + whereStr);
            
            // Query with Year
            whereStr     = " FROM raw WHERE collector_num = ? AND genus = ? AND `year` = ?";
            pCntStmts[1] = dbGBIFConn.prepareStatement("SELECT COUNT(*)" + whereStr);
            pStmts[1]    = dbGBIFConn.prepareStatement(gbifSQL + whereStr);

            process("analysis", "gbif", cntSQL, michSQL, pCntStmts, pStmts, 1, true);
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally 
        {
            try
            {
                for (int i=0;i<pCntStmts.length;i++)
                {
                    pCntStmts[i].close();
                    pStmts[i].close();
                }
            } catch (Exception ex)
            {
                
            }
            System.out.println("Done.");
        }

    }

    
    /* (non-Javadoc)
     * @see edu.ku.brc.specify.toycode.mexconabio.AnalysisBase#process(int, int)
     */
    public void process(final String baseDirName,
                        final String dataDirName,
                        final String cntSQL, 
                        final String mainSQL,
                        final PreparedStatement[] pCntStmts, 
                        final PreparedStatement[] pStmts,
                        final int numFieldForName,
                        final boolean isDateStr)
    {
        final double HRS = 1000.0 * 60.0 * 60.0; 
        Calendar cal = Calendar.getInstance();
        
        Statement srcStmt = null;
        
        long totalRecs     = BasicSQLUtils.getCountAsInt(dbSrcConn, cntSQL);
        long procRecs      = 0;
        long startTime     = System.currentTimeMillis();
        int  secsThreshold = 0;
        
        System.out.println("Starting... " + totalRecs);
        System.out.println(mainSQL);
        
        int maxScore = 0;
        for (int sc : MAXSCORES)
        {
            maxScore += sc;
        }
        
        try
        {
            String fileName = String.format("%s_%d_%d.html", dataDirName, procRecs, (procRecs+1000));
            String title    = String.format("%s 0 - 1000", dataDirName);
            startLogging(baseDirName, dataDirName, fileName, title);

            srcStmt  = dbSrcConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
            srcStmt.setFetchSize(Integer.MIN_VALUE);

            
            boolean isEvenRow = true;
            
            ResultSet rs = srcStmt.executeQuery(mainSQL);
            while (rs.next())
            {
                procRecs++;
                if (procRecs < 141) continue;
                
                Integer catNumInt    = rs.getInt(1);
                String  catNum       = catNumInt.toString();
                String  collectorNum = getStr(rs, 2);
                String  genus        = getStr(rs, 3);
                String  species      = getStr(rs, 4);
                String  subSpecies   = getStr(rs, 5);
                String  collector    = getStr(rs, 6);
                String  locality     = getStr(rs, 7);
                Date    collDate     = rs.getDate(8);
                String  country      = getStr(rs, 9);
                
                if (country != null && country.toLowerCase().equals("mexico"))
                {
                    country = "México";
                }
                
                int     year         = 0;
                int     mon          = 0;
                int     day          = 0;
                
                boolean hasDate;
                if (collDate != null)
                {
                    cal.setTime(collDate);
                    hasDate = true;
                    year    = cal.get(Calendar.YEAR);
                    mon     = cal.get(Calendar.MONTH) + 1;
                    day     = cal.get(Calendar.DAY_OF_MONTH);
                    
                } else
                {
                    hasDate= false;
                }
                
                int type  = 0;
                if (hasDate)
                {
                   type++;
                }
                
                PreparedStatement pStmt    = pStmts[type];
                PreparedStatement pCntStmt = pCntStmts[type];
                //System.out.println("["+collectorNum+"]["+genus+"]["+year+"]");

                pStmt.setString(1,    collectorNum);
                pCntStmt.setString(1, collectorNum);
                
                pStmt.setString(2,    genus);
                pCntStmt.setString(2, genus);
                
                if (hasDate)
                {
                    pStmt.setInt(3, year);
                    pCntStmt.setInt(3, year);
                }
                         
                int       cnt   = 0;
                ResultSet cntRS = pCntStmt.executeQuery();
                if (cntRS.next())
                {
                    cnt = cntRS.getInt(1);
                }
                cntRS.close();
                if (cnt < 1) continue;
                
                isEvenRow = !isEvenRow;
                
                //System.out.println(pStmt.toString());
                ResultSet gRS  = pStmt.executeQuery();
                int rowCnt = 0;
                while (gRS.next())
                {
                    for (int i=0;i<cCls.length;i++)
                    {
                        cCls[i] = null;
                        mCls[i] = null;
                    }
                    
                    String refCatNum     = getStr(gRS, 1);
                    String refColNum     = getStr(gRS, 2);
                    String refGenus      = getStr(gRS, 3);
                    String refSpecies    = getStr(gRS, 4);
                    String refSubSpecies = getStr(gRS, 5);
                    
                    int inx = 6;
                    String refLastNameF  = null;
                    String refLastNameM  = null;
                    String refFirstName  = null;
                    
                    String refCollector = null;
                    
                    switch (numFieldForName)
                    {
                        case 1 :
                            refLastNameF  = getStr(gRS, inx++);
                            refCollector  = refLastNameF;
                            break;
                            
                        case 2 :
                            refLastNameF  = getStr(gRS, inx++);
                            refFirstName  = getStr(gRS, inx++);
                            refCollector = String.format("%s %s", (StringUtils.isNotEmpty(refLastNameF) ? refLastNameF : ""), (StringUtils.isNotEmpty(refFirstName) ? refFirstName : "")).trim();
                            break;
                            
                        case 3 :
                            refLastNameF  = getStr(gRS, inx++);
                            refLastNameM  = getStr(gRS, inx++);
                            refFirstName  = getStr(gRS, inx++);
                            refCollector = String.format("%s %s %s", (StringUtils.isNotEmpty(refLastNameF) ? refLastNameF : ""), (StringUtils.isNotEmpty(refLastNameM) ? refLastNameM : ""), (StringUtils.isNotEmpty(refFirstName) ? refFirstName : "")).trim();
                            break;
                    }
                    
                    String refLocality   = getStr(gRS, inx++);
                    
                    inx += 2;
                    //String refLat        = getStr(gRS, inx++);
                    //String refLon        = getStr(gRS, inx++);

                    int     refYear;
                    int     refMon;
                    int     refDay;
                    
                    if (isDateStr)
                    {
                        String refYearStr      = getStr(gRS, inx++);
                        String refMonStr       = getStr(gRS, inx++);
                        String refDayStr       = getStr(gRS, inx++);
                        
                        refYear = StringUtils.isNotEmpty(refYearStr) && StringUtils.isNumeric(refYearStr) ? Integer.parseInt(refYearStr) : 0;
                        refMon  = StringUtils.isNotEmpty(refMonStr) && StringUtils.isNumeric(refMonStr) ? Integer.parseInt(refMonStr) : 0;
                        refDay  = StringUtils.isNotEmpty(refDayStr) && StringUtils.isNumeric(refDayStr) ? Integer.parseInt(refDayStr) : 0;
                        
                    } else
                    {
                        refYear      = gRS.getInt(inx++);
                        refMon       = gRS.getInt(inx++);
                        refDay       = gRS.getInt(inx++);
                        
                    }

                    String refCountry    = getStr(gRS, inx++);
                    
                    if (refCountry != null && refCountry.toLowerCase().equals("mexico"))
                    {
                        refCountry = "México";
                    }
                    
                    int score = 0;
                    
                    if (hasDate && refYear != 0)
                    {
                        score += compareDate(year, mon, day, refYear, refMon, refDay);
                    }
                    
                    double ratingLoc   = longStringCompare(refLocality,  locality, false);
                    double ratingColtr = longStringCompare(refCollector, collector, false);
                    
                    if (ratingLoc > 50.0)
                    {
                        cCls[LOCALITY_INX] = BGR;
                        mCls[LOCALITY_INX] = BGR;
                        score += 10;
                        
                    } else if (ratingLoc > 30.0)
                    {
                        cCls[LOCALITY_INX] = GR;
                        mCls[LOCALITY_INX] = GR;
                        score += 6;
                        
                    } else if (ratingLoc > 0.0)
                    {
                        cCls[LOCALITY_INX] = YW;
                        mCls[LOCALITY_INX] = YW;
                        score += 3;
                    }
                    
                    if (ratingColtr > 50.0)
                    {
                        cCls[COLLECTOR_INX] = BGR;
                        mCls[COLLECTOR_INX] = BGR;
                        score += 10;
                        
                    } else if (ratingColtr > 30.0)
                    {
                        cCls[COLLECTOR_INX] = GR;
                        mCls[COLLECTOR_INX] = GR;
                        score += 6;
                        
                    } else if (ratingColtr > 0.0)
                    {
                        cCls[COLLECTOR_INX] = YW;
                        mCls[COLLECTOR_INX] = YW;
                        score += 3;
                    }

                    
                    boolean genusMatches = false;
                    if (refGenus != null && genus != null)
                    {
                        if (refGenus.equals(genus))
                        {
                            score += 15;
                            cCls[GENUS_INX] = GR;
                            mCls[GENUS_INX] = GR;
                            genusMatches = true;
                            
                        } else if (StringUtils.getLevenshteinDistance(genus, refGenus) < 3)
                        {
                            score += 7;
                            cCls[GENUS_INX] = YW;
                            mCls[GENUS_INX] = YW;
                        }
                    }
                    
                    boolean speciesMatch = false;
                    if (refSpecies != null && species != null) 
                    {
                        if (refSpecies.equals(species))
                        {
                            score += 20;
                            speciesMatch = true;
                            if (genusMatches)
                            {
                                cCls[GENUS_INX] = BGR;
                                mCls[GENUS_INX] = BGR;
                                cCls[SPECIES_INX] = BGR;
                                mCls[SPECIES_INX] = BGR;
                            } else
                            {
                                cCls[SPECIES_INX] = GR;
                                mCls[SPECIES_INX] = GR;
                            }
                        } else if (StringUtils.getLevenshteinDistance(species, refSpecies) < 3)
                        {
                            score += 10;
                            cCls[SPECIES_INX] = YW;
                            mCls[SPECIES_INX] = YW;
                        }
                    }
                    
                    if (refSubSpecies != null && subSpecies != null) 
                    {
                        if (refSubSpecies.equals(subSpecies))
                        {
                            score += 20;
                            if (genusMatches && speciesMatch)
                            {
                                cCls[GENUS_INX]      = BGR;
                                mCls[GENUS_INX]      = BGR;
                                cCls[SPECIES_INX]    = BGR;
                                mCls[SPECIES_INX]    = BGR;
                                cCls[SUBSPECIES_INX] = BGR;
                                mCls[SUBSPECIES_INX] = BGR;
                                
                            } else if (speciesMatch)
                            {
                                cCls[SPECIES_INX]    = BGR;
                                mCls[SPECIES_INX]    = BGR;
                                cCls[SUBSPECIES_INX] = BGR;
                                mCls[SUBSPECIES_INX] = BGR;
                                
                            } else
                            {
                                cCls[SPECIES_INX] = GR;
                                mCls[SPECIES_INX] = GR;
                            }
                        } else if (StringUtils.getLevenshteinDistance(subSpecies, refSubSpecies) < 3)
                        {
                            score += 10;
                            cCls[SUBSPECIES_INX] = YW;
                            mCls[SUBSPECIES_INX] = YW;
                        }
                    }
                    
                    if (refCountry != null && country != null) 
                    {
                        if (refCountry.equals(country))
                        {
                            cCls[COUNTRY_INX] = BGR;
                            mCls[COUNTRY_INX] = BGR;
                            score += 10;
                            
                        } else if (StringUtils.getLevenshteinDistance(country, refCountry) < 3)
                        {
                            score += 5;
                            cCls[SUBSPECIES_INX] = YW;
                            mCls[SUBSPECIES_INX] = YW;
                        }
                    }
                    
                    /*if (refGenus != null && species != null && refGenus.equals(species))     
                    {
                        cCls[3] = DF;
                        mCls[2] = DF;                        
                        score += 10;
                    }
                    
                    if (refSpecies != null && genus != null && refSpecies.equals(genus))
                    {
                        cCls[2] = DF;
                        mCls[3] = DF;
                        score += 15;
                    }*/
                    
                    if (catNum.equals(refCatNum))
                    {
                        cCls[CATNUM_INX] = BGR;
                        mCls[CATNUM_INX] = BGR;
                    }
                    
                    if (collectorNum.equals(refColNum))
                    {
                        cCls[COLNUM_INX] = BGR;
                        mCls[COLNUM_INX] = BGR;
                        score += 10;
                    }
                    
                    int mxSc = maxScore + (refSubSpecies == null && subSpecies == null ? -20 : 0);
                    int finalScore = (int)((((double)score / mxSc) * 100.0)+0.5);
                    String scoreStr = String.format("%d", finalScore);
                    
                    if (rowCnt == 0)
                    {
                        tblWriter.println("<TR><TD colspan=\""+cCls.length+"\">&nbsp;</TD></TR>\n");
                        tblWriter.println("<TR class=\""+(isEvenRow ? "evh" : "odh")+"\">");
                        tblWriter.logTDCls(cCls[CATNUM_INX],     catNum);
                        tblWriter.logTDCls(cCls[COLNUM_INX],     collectorNum);
                        tblWriter.logTDCls(cCls[GENUS_INX],      genus);
                        tblWriter.logTDCls(cCls[SPECIES_INX],    species);
                        tblWriter.logTDCls(cCls[SUBSPECIES_INX], "");
                        tblWriter.logTDCls(cCls[COLLECTOR_INX],  collector);
                        tblWriter.logTDCls(cCls[LOCALITY_INX],   locality);
                        tblWriter.logTDCls(cCls[LATITUDE_INX],   null);
                        tblWriter.logTDCls(cCls[LONGITUDE_INX],  null);
                        tblWriter.logTDCls(cCls[YEAR_INX],       Integer.toString(year));
                        tblWriter.logTDCls(cCls[MON_INX],        Integer.toString(mon));
                        tblWriter.logTDCls(cCls[DAY_INX],        Integer.toString(day));
                        tblWriter.logTDCls(cCls[COUNTRY_INX],    country);
                        tblWriter.logTDCls(cCls[SCORE_INX],      null);
                        tblWriter.println("</TR>\n");
                    }
                    
                    tblWriter.println("<TR class=\""+(isEvenRow ? "ev" : "od")+"\">");
                    tblWriter.logTDCls(cCls[CATNUM_INX],    refCatNum);
                    tblWriter.logTDCls(cCls[COLNUM_INX],    refColNum);
                    tblWriter.logTDCls(cCls[GENUS_INX],     refGenus);
                    tblWriter.logTDCls(cCls[SPECIES_INX],   refSpecies);
                    tblWriter.logTDCls(cCls[SUBSPECIES_INX], null);
                    tblWriter.logTDCls(cCls[COLLECTOR_INX], refLastNameF);
                    tblWriter.logTDCls(cCls[LOCALITY_INX],  refLocality);
                    tblWriter.logTDCls(cCls[LATITUDE_INX],  null);
                    tblWriter.logTDCls(cCls[LONGITUDE_INX], null);
                    tblWriter.logTDCls(cCls[YEAR_INX],      Integer.toString(refYear));
                    tblWriter.logTDCls(cCls[MON_INX],       Integer.toString(refMon));
                    tblWriter.logTDCls(cCls[DAY_INX],       Integer.toString(refDay));
                    tblWriter.logTDCls(cCls[COUNTRY_INX],   refCountry);
                    tblWriter.logTDCls(cCls[SCORE_INX],     scoreStr);
                    tblWriter.println("</TR>\n");
                    rowCnt++;
                }
                
                //procRecs++;
                //if (procRecs > 200) return;
                
                if (procRecs % 1000 == 0)
                {
                    title    = String.format("%s %d - %d",    dataDirName, procRecs, (procRecs+1000));
                    fileName = String.format("%s_%d_%d.html", dataDirName, procRecs, (procRecs+1000));
                            
                    startNewDocument(fileName, title, true);
                    
                    long endTime     = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    
                    double timePerRecord      = (elapsedTime / procRecs); 
                    
                    double hrsLeft = ((totalRecs - procRecs) * timePerRecord) / HRS;
                    
                    int seconds = (int)(elapsedTime / 60000.0);
                    if (secsThreshold != seconds)
                    {
                        secsThreshold = seconds;
                        
                        String msg = String.format("Elapsed %8.2f hr.mn   Percent: %6.3f  Hours Left: %8.2f ", 
                                ((double)(elapsedTime)) / HRS, 
                                100.0 * ((double)procRecs / (double)totalRecs),
                                hrsLeft);
                        System.out.println(msg);
                    }
                }
            }
            rs.close();
            
            System.out.println("Done.");
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
            
        } finally 
        {
            try
            {
                if (srcStmt != null)
                {
                    srcStmt.close();
                }
                
            } catch (Exception ex) {}
            System.out.println("Done.");
        }
        System.out.println("Done.");
    }
    
    
    //------------------------------------------------------------------------------------------
    public static void main(String[] args)
    {
        AnalysisWithSNIB awg = new AnalysisWithSNIB();
        awg.createDBConnection("localhost",     "3306", "gbif",           "root", "root");
        awg.createSrcDBConnection("localhost",  "3306", "mex",            "root", "root");
        
        //awg.createDestDBConnection("localhost", "3306", "analysis_cache", "root", "root");
        awg.process(1, DO_ALL);
        awg.endLogging();
        
        awg.cleanup();
    }
}