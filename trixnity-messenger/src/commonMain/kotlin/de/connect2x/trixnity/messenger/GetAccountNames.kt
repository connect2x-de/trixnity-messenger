package de.connect2x.trixnity.messenger

interface GetAccountNames {
    suspend operator fun invoke(): List<String>
}

class GetAccountNamesImpl : GetAccountNames {
    override suspend operator fun invoke(): List<String> {
        return getAccountNames()
    }
}