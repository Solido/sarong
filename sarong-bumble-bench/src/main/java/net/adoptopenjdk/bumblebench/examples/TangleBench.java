/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package net.adoptopenjdk.bumblebench.examples;

import net.adoptopenjdk.bumblebench.core.MicroBench;
import sarong.TangleRNG;

/**
 * TangleBench score: 1201761280.000000 (1.202G 2090.7%)
 *         uncertainty:   0.2%
 * <br>
 * OpenJ9 OpenJDK 13 on linux:
 * <br>
 * TangleBench score: 3837962240.000000 (3.838G 2206.8%)
 *         uncertainty:   1.3%
 */
public final class TangleBench extends MicroBench {

	protected long doBatch(long numIterations) throws InterruptedException {
		TangleRNG rng = new TangleRNG(0x1234L, 5678L);
		long sum = 0L;
		for (long i = 0; i < numIterations; i++)
			sum += rng.nextLong();
		return numIterations;
	}
}

