package com.bodyweight.fitness.stream

import com.bodyweight.fitness.App
import com.bodyweight.fitness.model.*
import com.bodyweight.fitness.utils.Preferences

import com.google.gson.Gson

import org.apache.commons.io.IOUtils

import java.io.IOException

import com.bodyweight.fitness.R
import com.bodyweight.fitness.adapter.SpinnerRoutine
import com.bodyweight.fitness.extension.debug

import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject

class JsonRoutineLoader {
    fun getRoutine(resource: Int): Routine {
        try {
            val raw = IOUtils.toString(App.context!!.resources.openRawResource(resource))
            val jsonRoutine = Gson().fromJson(raw, JSONRoutine::class.java)

            return Routine(jsonRoutine)
        } catch (e: IOException) {
            error(e.message.toString())
        }
    }
}

object RoutineStream {
    private val routineSubject = PublishSubject.create<Routine>()
    private val exerciseSubject = PublishSubject.create<Exercise>()

    val idToRoutine = mapOf("routine0" to R.raw.bodyweight_fitness_recommended_routine,
        "d8a722a0-fae2-4e7e-a751-430348c659fe" to R.raw.starting_stretching_flexibility_routine,
        "e73593f4-ee17-4b9b-912a-87fa3625f63d" to R.raw.molding_mobility_flexibility_routine,
        "97dd5a35-ed82-4c89-82e0-715f1177578a" to R.raw.wrist_mobility_flexibility_routine,
        "2ef7e839-7b0d-4963-ad8b-637e99d70230" to R.raw.handstand_push_up_routine);

    var routine: Routine = JsonRoutineLoader().getRoutine(idToRoutine[Preferences.defaultRoutine]
            ?: R.raw.bodyweight_fitness_recommended_routine)

        set(value) {
            if (value.routineId.equals(routine.routineId)) {
                return
            }

            Preferences.defaultRoutine = value.routineId

            exercise = value.linkedExercises.first()

            routineSubject.onNext(value)

            field = value

            debug("set value of: " + routine.title)
        }

    var exercise: Exercise = routine.linkedExercises.first()
        set(value) {
            exerciseSubject.onNext(value)

            field = value
        }

    fun setRoutine(spinnerRoutine: SpinnerRoutine) {
        when(spinnerRoutine.id) {
            0 -> {
                routine = JsonRoutineLoader().getRoutine(R.raw.bodyweight_fitness_recommended_routine)
            }
            1 -> {
                routine = JsonRoutineLoader().getRoutine(R.raw.starting_stretching_flexibility_routine)
            }
            2 -> {
                routine = JsonRoutineLoader().getRoutine(R.raw.molding_mobility_flexibility_routine)
            }
            3 -> {
                routine = JsonRoutineLoader().getRoutine(R.raw.wrist_mobility_flexibility_routine)
            }
            4 -> {
                routine = JsonRoutineLoader().getRoutine(R.raw.handstand_push_up_routine)
            }
        }
    }

    fun routineObservable(): Observable<Routine> {
        return Observable.merge(Observable.just(routine).publish().refCount(), routineSubject)
                .observeOn(AndroidSchedulers.mainThread())
                .publish()
                .refCount()
    }

    fun exerciseObservable(): Observable<Exercise> {
        return Observable.merge(Observable.just(exercise).publish().refCount(), exerciseSubject)
                .observeOn(AndroidSchedulers.mainThread())
                .publish()
                .refCount()
    }

    fun setLevel(chosenExercise: Exercise, level: Int) {
        routine.setLevel(chosenExercise, level)

        exercise = chosenExercise

        val sectionId = chosenExercise.section!!.sectionId
        val exerciseId = chosenExercise.section!!.currentExercise.exerciseId

        Preferences.setExerciseIdForSection(sectionId, exerciseId)
    }
}
