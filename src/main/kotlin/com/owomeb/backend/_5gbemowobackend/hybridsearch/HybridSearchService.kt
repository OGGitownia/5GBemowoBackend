package com.owomeb.backend._5gbemowobackend.hybridsearch

import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*

@Service
@RestController

class HybridSearchService : PythonServerModel<String>(
    scriptPath = "src/main/resources/pythonScripts/hybridsearch/serverSearch.py",
    serverName = "hybrid_search_server",
    autoClose = false
)
