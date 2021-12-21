/*
 * This file is part of FragPipe.
 *
 * FragPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FragPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FragPipe.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dmtavt.fragpipe.process;

import com.dmtavt.fragpipe.cmd.ProcessBuilderInfo;

public class RunnableDescription {

  public final ProcessDescription description;
  public final Runnable runnable;
  public final String parallelGroup;
  public final ProcessBuilderInfo pbi;

  public RunnableDescription(ProcessDescription description,
      Runnable runnable) {
    this(description, runnable, null, null);
  }

  public RunnableDescription(
          ProcessDescription description,
          Runnable runnable, String parallelGroup, ProcessBuilderInfo pbi) {
    this.description = description;
    this.runnable = runnable;
    this.parallelGroup = parallelGroup;
    this.pbi = pbi;
  }
}
