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

package org.apache.druid.indexing.kinesis;


import org.apache.druid.indexing.seekablestream.SeekableStreamPartitions;
import org.apache.druid.indexing.seekablestream.common.OrderedSequenceNumber;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;

public class KinesisSequenceNumber extends OrderedSequenceNumber<String>
{

  /**
   * In Kinesis, when a shard is closed due to shard splitting, a null ShardIterator is returned.
   * The EOS marker is placed at the end of the Kinesis Record Supplier buffer, such that when
   * an indexing task pulls the record 'EOS', it knows the shard has been closed and should stop
   * reading and start publishing
   */
  public static final String END_OF_SHARD_MARKER = "EOS";
  // this flag is used to indicate either END_OF_SHARD_MARKER
  // or NO_END_SEQUENCE_NUMBER so that they can be properly compared
  // with other sequence numbers
  private final boolean isMaxSequenceNumber;
  private final BigInteger intSequence;

  private KinesisSequenceNumber(@NotNull String sequenceNumber, boolean isExclusive)
  {
    super(sequenceNumber, isExclusive);
    if (END_OF_SHARD_MARKER.equals(sequenceNumber)
        || SeekableStreamPartitions.NO_END_SEQUENCE_NUMBER.equals(sequenceNumber)) {
      isMaxSequenceNumber = true;
      this.intSequence = null;
    } else {
      isMaxSequenceNumber = false;
      this.intSequence = new BigInteger(sequenceNumber);
    }
  }

  public static KinesisSequenceNumber of(String sequenceNumber)
  {
    return new KinesisSequenceNumber(sequenceNumber, false);
  }

  public static KinesisSequenceNumber of(String sequenceNumber, boolean isExclusive)
  {
    return new KinesisSequenceNumber(sequenceNumber, isExclusive);
  }

  @Override
  public int compareTo(@NotNull OrderedSequenceNumber<String> o)
  {
    KinesisSequenceNumber num = (KinesisSequenceNumber) o;
    if (isMaxSequenceNumber && num.isMaxSequenceNumber) {
      return 0;
    } else if (isMaxSequenceNumber) {
      return 1;
    } else if (num.isMaxSequenceNumber) {
      return -1;
    } else {
      return this.intSequence.compareTo(new BigInteger(o.get()));
    }
  }

}
