/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysml.runtime.controlprogram.paramserv;

import org.apache.sysml.parser.Statement;
import org.apache.sysml.runtime.DMLRuntimeException;
import org.apache.sysml.runtime.controlprogram.context.ExecutionContext;
import org.apache.sysml.runtime.instructions.cp.Data;
import org.apache.sysml.runtime.instructions.cp.ListObject;

public class LocalParamServer extends ParamServer {

	public LocalParamServer(ListObject model, String aggFunc, Statement.PSFrequency freq,
			Statement.PSUpdateType updateType, ExecutionContext ec, int workerNum,
			ListObject hyperParams) {
		super(model, aggFunc, freq, updateType, ec, workerNum, hyperParams);
	}

	@Override
	public void push(long workerID, ListObject gradients) {
		synchronized (_lock) {
			_queue.add(new Gradient(workerID, gradients));
			_lock.notifyAll();
		}
	}

	@Override
	public Data pull(long workerID) {
		synchronized (_lock) {
			while (getPulledState((int) workerID)) {
				try {
					_lock.wait();
				} catch (InterruptedException e) {
					throw new DMLRuntimeException(
							String.format("Local worker_%d: failed to pull the global parameters.", workerID), e);
				}
			}
			setPulledState((int) workerID, true);
		}
		return getResult();
	}
}
