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
var filter = {
	logLevel: "INFO",
	source: null,
	search: null,
	startDate: 0,
	endDate: null,
};

/**
 * @type LogEntry[]
 */
var allLogs = [];
var oldestLogFileFetched = null;

/** @type {const} */
const LogLevels = ["ERROR", "INFO", "DEBUG"];

/** @typedef {'ERROR' | 'WARNING' | 'INFO' | 'DEBUG'} LogLevel */

/**
 * @typedef {Object} LogEntry
 * @property {number} time
 * @property {string} source
 * @property {string} message
 * @property {LogLevel} level
 * @property {string} [exception]
 */

const formatTimestamp = (date) =>
	new Date(date).toLocaleString([], {
		year: "numeric",
		month: "short",
		day: "2-digit",
		hour: "2-digit",
		minute: "2-digit",
		second: "2-digit",
	});

/**
 * @type {(logFile: string) => LogEntry[]}
 *
 * Parse a log file into a list of log entries
 *
 * @param logFile the contents of a log file
 */
const parseLogs = (logFile) => {
	const logLines = logFile.trim().split("\n");
	return logLines.map((log) => {
		try {
			return JSON.parse(log);
		} catch (e) {
			return {
				time: null,
				source: null,
				message: log,
				level: "INFO",
				exception: null,
			};
		}
	});
};

const populateSourceFilter = () => {
	$("#sourceFilter").empty();
	$("#sourceFilter").append("<option>Any</option>");
	const sources = {};
	for (const log of allLogs) {
		if (log.source && sources[log.source] !== 1) {
			$("#sourceFilter").append(`<option>${log.source}</option>`);
			sources[log.source] = 1;
		}
	}
};

/**
 * Update the filter based on the log filter form
 */
const onFilterChange = () => {
	const logLevel = $("#logLevelFilter").val();
	filter.logLevel = LogLevels.includes(logLevel) ? logLevel : null;

	filter.search = $("#searchFilter").val().toLowerCase();

	filter.source = $("#sourceFilter").val();

	const start = $("#startFilter").val();
	const end = $("#endFilter").val();
	filter.startDate = start !== "" ? Date.parse(start) : null;
	filter.endDate = end !== "" ? Date.parse(end) : null;

	prettyPrintLogs($("#TextArea"));
};

const getFirstAndLastLogEntry = (start, end) => {
	let maxStart = allLogs.length;
	let minStart = 0;
	if (start !== null) {
		while (maxStart !== minStart) {
			const bisection = Math.floor((maxStart + minStart) / 2);
			if (allLogs[bisection].time < start) {
				minStart = bisection + 1;
			} else {
				maxStart = bisection;
			}
		}
	} else {
		maxStart = 0;
	}

	let maxEnd = allLogs.length;
	let minEnd = maxStart;
	if (end !== null) {
		while (maxEnd !== minEnd) {
			const bisection = Math.floor((maxEnd + minEnd) / 2);
			if (allLogs[bisection].time < end) {
				minEnd = bisection + 1;
			} else {
				maxEnd = bisection;
			}
		}
	} else {
		maxEnd = allLogs.length;
	}

	return [maxStart, maxEnd];
};

/**
 *
 * @param {LogEntry[]} logs
 * @param {*} filter
 * @returns
 */
const filterLogs = (logs, filter) => {
	const [start, end] = getFirstAndLastLogEntry(
		filter.startDate,
		filter.endDate,
	);
	const logSlice = logs.slice(start, end);

	return logSlice.filter((log) => {
		if (
			filter.logLevel &&
			LogLevels.indexOf(filter.logLevel) < LogLevels.indexOf(log.level)
		) {
			return false;
		}

		if (filter.search && filter.search.length > 0) {
			if (!log.message.toLowerCase().includes(filter.search)) {
				return false;
			}
		}

		if (filter.source && filter.source !== "Any") {
			if (log.source !== filter.source) {
				return false;
			}
		}

		return true;
	});
};

/**
 * @type {(message: string) => void}
 *
 * Copy text to the clipboard
 *
 * @param message the text to copy
 */
const copy = (message) => {
	navigator.clipboard.writeText(message);
};

/**
 * @type {(logs: LogEntry[], div: JQuery<HTMLElement>) => void}
 *
 * Add each log entry to the provided div.
 *
 * @param logs a list of log entries
 * @param div  a div to append the log entries to
 */
const prettyPrintLogs = (div) => {
	div.empty();
	for (const log of filterLogs(allLogs, filter)) {
		div.append(`<div class="logEntry ${log.level}">
						<div class="timestamp">${log.time ? formatTimestamp(log.time) : ""}</div>
						<div class="message">${log.application ? `${log.application}: ` : ""}${log.message}</div>
						<div class="level">(${log.level})</div>
						${
							log.exception
								? `<div class="exceptionPill">Stack Trace</div>`
								: ""
						}
						<button class="copyIcon" onclick="copy('${formatTimestamp(log.time)} ${log.application ? `${log.application}: ` : ""}${log.message} (${log.level})')">&#x1F4CB;</button>
					</div>`);
	}
};

/**
 * @type {(result: string) => void}
 *
 * Show the System Information as it is currently executing in the WebService
 *
 * @param result the id of the HTML tag to insert the log into
 */
function showInfo(result) {
	$(".filters").css("visibility", "hidden");
	const infoDiv = $("#" + result);
	$.get(appRoot + "admin/Info", function (data) {
		infoDiv.empty();
		infoDiv.append(data);
	})
		.fail(function (data) {
			console.log("error");
			infoDiv.empty();
			infoDiv.append(data);
		})
		.always(function () {
			console.log("complete");
		});
}

/**
 * @type {(result: string) => void}
 *
 * Show the contents of the latest LogFile
 *
 * @param result the id of the HTML tag to insert the log into
 */
const showLog = (result, file = null) => {
	const logDiv = $("#" + result);
	$.get(
		appRoot + `admin/Log${file !== null ? `?file=${file}` : ""}`,
		function (data, status, xhr) {
			// Grab the current scroll height so we can set the scroll height right after adding new log entries.
			const oldScroll = $(logDiv.parent()).scrollTop();
			const oldHeight = $(logDiv.parent())[0].scrollHeight;

			const newLogs = parseLogs(data);
			if (file !== null) {
				allLogs = newLogs.concat(allLogs);
			} else {
				allLogs = newLogs;
			}
			oldestLogFileFetched = xhr.getResponseHeader("logFile");
			if (data.length >= 0 && oldestLogFileFetched > 0) {
				$("#loadMore").css("visibility", "visible");
			} else {
				$("#loadMore").css("visibility", "hidden");
			}
			populateSourceFilter();
			prettyPrintLogs(logDiv);

			$(logDiv.parent()).scrollTop(
				logDiv.parent()[0].scrollHeight - oldHeight + oldScroll,
			);

			$("#logFilters").css("visibility", "visible");
		},
	)
		.fail(function (data) {
			console.log("error");
			logDiv.val(data);
		})
		.always(function () {
			console.log("complete");
		});
};

const loadMore = () => {
	showLog("TextArea", oldestLogFileFetched ? oldestLogFileFetched - 1 : null);
};
