/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _IGNITE_ODBC_DRIVER_QUERY
#define _IGNITE_ODBC_DRIVER_QUERY

#include <stdint.h>

#include <map>

#include "ignite/odbc/meta/column_meta.h"
#include "ignite/odbc/common_types.h"
#include "ignite/odbc/row.h"

namespace ignite
{
    namespace odbc
    {
        namespace query
        {
            /**
             * Query.
             */
            class Query
            {
            public:
                /**
                 * Constructor.
                 */
                Query() 
                {
                    // No-op.
                }

                /**
                 * Destructor.
                 */
                virtual ~Query()
                {
                    // No-op.
                }

                /**
                 * Execute query.
                 *
                 * @return True on success.
                 */
                virtual bool Execute() = 0;

                /**
                 * Get column metadata.
                 *
                 * @return Column metadata.
                 */
                virtual const meta::ColumnMetaVector& GetMeta() const = 0;

                /**
                 * Fetch next result row to application buffers.
                 *
                 * @return Operation result.
                 */
                virtual SqlResult FetchNextRow(app::ColumnBindingMap& columnBindings) = 0;

                /**
                 * Close query.
                 *
                 * @return True on success.
                 */
                virtual bool Close() = 0;

                /**
                 * Check if data is available.
                 *
                 * @return True if data is available.
                 */
                virtual bool DataAvailable() const = 0;
            };
        }
    }
}

#endif