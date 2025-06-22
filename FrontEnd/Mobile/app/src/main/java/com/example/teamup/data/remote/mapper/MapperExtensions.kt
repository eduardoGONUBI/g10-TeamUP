package com.example.teamup.data.remote.mapper

import com.example.teamup.data.remote.model.*
import com.example.teamup.domain.model.*

// === DTO → Domain mappers ===

internal fun SportDto.toDomain(): Sport = Sport(
    id   = id,
    name = name
)

internal fun AchievementDto.toDomain(): Achievement = Achievement(
    code        = code,
    title       = title,
    description = description,
    icon        = icon
)

internal fun AchievementsResponse.toDomain(): List<Achievement> =
    achievements.map { it.toDomain() }

internal fun ReputationResponse.toDomain(): Reputation = Reputation(
    goodTeammate = good_teammate_count,
    friendly     = friendly_count,
    teamPlayer   = team_player_count,
    toxic        = toxic_count,
    badSport     = bad_sport_count,
    afk          = afk_count
)

internal fun ProfileResponse.toDomain(reputation: Reputation): ProfileStats = ProfileStats(
    xp         = xp,
    level      = level,
    reputation = reputation
)

internal fun FeedbackRequestDto.toDomain(): FeedbackRequest = FeedbackRequest(
    userId    = userId,
    attribute = attribute
)

internal fun ParticipantDto.toDomain(): User = User(
    id        = id,
    name      = name,
    email     = null,
    location  = null,
    latitude  = null,
    longitude = null,
    sports    = null,
    avatarUrl = null
)

internal fun CreatorDto.toDomain(): User = User(
    id        = id,
    name      = name,
    email     = null,
    location  = null,
    latitude  = null,
    longitude = null,
    sports    = null,
    avatarUrl = null
)

internal fun UserDto.toDomain(): User = User(
    id        = id,
    name      = name,
    email     = email,
    location  = location,
    latitude  = latitude,
    longitude = longitude,
    sports    = sports?.map { it.toDomain() },
    avatarUrl = avatarUrl
)

internal fun PublicUserDto.toDomain(): User = User(
    id        = id,
    name      = name,
    email     = null,
    location  = location,
    latitude  = latitude,
    longitude = longitude,
    sports    = sports?.map { it.toDomain() },
    avatarUrl = avatar_url
)

internal fun WeatherDto.toDomain(): Weather = Weather(
    temp        = temp,
    highTemp    = high_temp,
    lowTemp     = low_temp,
    description = description
)

internal fun ActivityDto.toDomain(currentUserId: Int): Activity = Activity(
    id              = id.toString(),
    title           = "$name : $sport",
    location        = place,
    startsAt        = startsAt ?: date.orEmpty(),
    participants    = participants?.size ?: 0,
    maxParticipants = max_participants,
    organizer       = creator.name,
    creatorId       = creator.id,
    isParticipant   = participants?.any { it.id == currentUserId } ?: false,
    latitude        = latitude,
    longitude       = longitude,
    isCreator       = (creator.id == currentUserId),
    status          = status
)

internal fun CreateEventRawDto.toDomain(currentUserId: Int): Activity = Activity(
    id              = id.toString(),
    title           = name,
    location        = place,
    startsAt        = startsAt,
    participants    = 1,
    maxParticipants = maxParticipants,
    organizer       = userName,
    creatorId       = userId,
    isParticipant   = true,
    latitude        = latitude,
    longitude       = longitude,
    isCreator       = (userId == currentUserId),
    status          = status
)

internal fun CreateEventRequestDomain.toDto() = CreateEventRequestDto(
    name            = name,
    sportId         = sportId,
    place           = place,
    maxParticipants = maxParticipants,
    startsAt        = startsAt,
    latitude        = latitude,
    longitude       = longitude
)

internal fun CreateEventRawResponse.toDomain(currentUserId: Int): Activity =
    event.toDomain(currentUserId)

internal fun <T, R> PaginatedResponse<T>.toDomain(mapper: (T) -> R): Page<R> = Page(
    data       = data.map(mapper),
    page       = meta.currentPage,
    totalPages = meta.lastPage,
    totalItems = meta.total
)


internal fun MessageDto.toDomain(currentUserId: Int): Message =
    Message(
        id        = id,
        eventId   = eventId,
        userId    = userId,
        author    = author,
        text      = text,
        timestamp = timestamp,
        fromMe    = (userId == currentUserId)
    )

// === Domain → DTO mappers (para enviar para API) ===

internal fun UpdateUserRequestDomain.toDto(): UpdateUserRequest = UpdateUserRequest(
    name      = this.name,
    email     = this.email,
    location  = this.location,
    latitude  = this.latitude,
    longitude = this.longitude,
    sports    = this.sports
)

internal fun RegisterRequestDomain.toDto(): RegisterRequestDto = RegisterRequestDto(
    name                 = this.name,
    email                = this.email,
    password             = this.password,
    passwordConfirmation = this.passwordConfirmation,
    location             = this.location
)


internal fun String.toFeedbackSlug(): String = when (trim().lowercase()) {
    "friendly"        -> "friendly"
    "good teammate"   -> "good_teammate"
    "team player"     -> "team_player"
    "toxic"           -> "toxic"
    "bad sport"       -> "bad_sport"
    "afk"             -> "afk"
    else              -> "friendly"
}

