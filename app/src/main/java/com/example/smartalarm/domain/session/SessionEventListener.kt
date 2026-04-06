package com.example.smartalarm.domain.session

import com.example.smartalarm.data.model.SleepSession

interface SessionEventListener {
    fun onSessionStateChanged(state: SessionState)
    fun onSessionFinished(session: SleepSession)
}