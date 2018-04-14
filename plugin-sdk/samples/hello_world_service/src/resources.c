/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
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
 */

#include <stdlib.h>
#include "plugin_sdk.h"
#include "er-coap-constants.h"

bool res_hello_handler (restful_request_t *request, restful_response_t * response)
{
	response->code =CONTENT_2_05;
	response->payload_fmt = IA_TEXT_PLAIN;
	response->payload = strdup("hello");
	response->payload_len = strlen(response->payload) + 1;

	printf("hello service: returned\n");

	return true;
}
