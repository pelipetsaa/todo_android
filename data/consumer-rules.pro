# DTOs are (de)serialized by kotlinx.serialization through generated serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.pelypets.todolist.data.remote.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
