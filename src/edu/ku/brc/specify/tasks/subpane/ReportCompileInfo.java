/* Copyright (C) 2020, Specify Collections Consortium
 * 
 * Specify Collections Consortium, Biodiversity Institute, University of Kansas,
 * 1345 Jayhawk Boulevard, Lawrence, Kansas, 66045, USA, support@specifysoftware.org
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
package edu.ku.brc.specify.tasks.subpane;

import java.io.File;

/**
 * @author rod
 *
 * @code_status Alpha
 *
 * Apr 29, 2007
 *
 */
public class ReportCompileInfo
{
    protected File    reportFile;
    protected File    compiledFile;
    protected boolean needsCompiled;
    
    public ReportCompileInfo(final File reportFile, final File compiledFile, final boolean needsCompiled)
    {
        super();
        this.reportFile    = reportFile;
        this.compiledFile  = compiledFile;
        this.needsCompiled = needsCompiled;
    }

    public File getCompiledFile()
    {
        return compiledFile;
    }

    public boolean isCompiled()
    {
        return !needsCompiled;
    }

    public File getReportFile()
    {
        return reportFile;
    }
}

