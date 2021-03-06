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

#include "module_entry.h"

extern bool res_hello_handler (restful_request_t *request, restful_response_t * response);



void on_init_idrm_plugin(void * framework)
{
    idrm_register_resource_handler(framework, "/hello", res_hello_handler, T_Get);
    return;
}


const MODULE_API_1 * on_get_user_module_apis()
{
	return NULL;
}

char * on_get_module_name()
{
	return "hello";
}


