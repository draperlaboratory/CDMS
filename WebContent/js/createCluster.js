/* =============================================================================
 * Vader Modular Fuzzer (VMF)
 * Copyright (c) 2021-2026 The Charles Stark Draper Laboratory, Inc.
 * <vmf@draper.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 (only) as 
 * published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *  
 * @license GPL-2.0-only <https://spdx.org/licenses/GPL-2.0-only.html>
 * ===========================================================================*/
/*************************************************************************************************************************************
 *
 *  Save a new Cluster to the Server
 *
 *************************************************************************************************************************************/
const newCluster = (e, title, description) => {
	if (isEmpty(title)) {
		console.log("Empty Cluster Title");
		return;
	}

	var max_title = 25;
	var max_desc = 50;

	if (title.length > max_title) {
		title = title.substr(0, max_title);
	}

	if (!isEmpty(description)) {
		if (description.length > max_desc) {
			description = description.substr(0, max_desc);
		}
	}

	var args = "Title=" + title + "&Description=" + description;

	$.getJSON(
		appRoot + "campaign/create/cluster",
		args,

		function (data) {
			getClusters();

			return false;
		},
	);

	return false;
};
