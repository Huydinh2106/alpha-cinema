package com.example.alphacinema

object MovieDetailFakeData {
    val movies: List<MovieDetailUi> = listOf(
        MovieDetailUi(
            id = "alice-in-borderland",
            title = "Thế Giới Không Lối Thoát",
            subtitle = "Alice in Borderland",
            year = "2020",
            ageRating = "T18",
            currentEpisode = "Tập 6",
            genres = listOf("Sinh tồn", "Tâm lý", "Kỳ ảo"),
            description = "Một game thủ lông bông cùng hai người bạn nhận ra họ đã lạc vào thế giới Tokyo song song, nơi buộc phải vượt qua các trò chơi chết chóc để sống sót. Càng đi sâu, họ càng phát hiện bí mật đen tối đằng sau người tổ chức và lý do thật sự của thế giới này.",
            bannerUrl = "https://m.media-amazon.com/images/M/MV5BNGEyOGJiNmEtMmI1OC00MDI4LWIxNzctYzg2YjYyMTk3MTdiXkEyXkFqcGc@._V1_.jpg",
            posterUrl = "https://m.media-amazon.com/images/M/MV5BNGEyOGJiNmEtMmI1OC00MDI4LWIxNzctYzg2YjYyMTk3MTdiXkEyXkFqcGc@._V1_.jpg",
            episodes = listOf(
                EpisodeUi("alice-ep-1", "Tập 1: Vào cuộc", "46 phút"),
                EpisodeUi("alice-ep-2", "Tập 2: Trò chơi bắt đầu", "52 phút"),
                EpisodeUi("alice-ep-3", "Tập 3: Lá bài đầu tiên", "48 phút"),
                EpisodeUi("alice-ep-4", "Tập 4: Cái giá phải trả", "50 phút"),
                EpisodeUi("alice-ep-5", "Tập 5: Sự thật lộ diện", "55 phút"),
                EpisodeUi("alice-ep-6", "Tập 6: Không còn đường lùi", "53 phút")
            ),
            cast = listOf(
                CastUi("Kento Yamazaki", "Arisu"),
                CastUi("Tao Tsuchiya", "Usagi"),
                CastUi("Nijiro Murakami", "Chishiya"),
                CastUi("Aya Asahina", "Kuina")
            ),
            recommendations = listOf(
                RecommendedMovieUi(
                    id = "squid-game",
                    title = "Squid Game",
                    year = "2021",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYWE3MDVkN2EtNjQ5MS00ZDQ4LTliNzYtMjc2YWMzMDEwMTA3XkEyXkFqcGdeQXVyMTEzMTI1Mjk3._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "dark",
                    title = "Dark",
                    year = "2017",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BOTk2NzUyOTctZDdlMS00MDJlLTgzNTEtNzQzYjFhNzY0ZGFmXkEyXkFqcGdeQXVyMjg1NDcxNDE@._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "the-platform",
                    title = "The Platform",
                    year = "2019",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMjAzMjlhNGQtMjAzZC00ODIzLWE4YTAtMDkyYmFiZmE2YWYzXkEyXkFqcGc@._V1_.jpg"
                )
            )
        ),
        MovieDetailUi(
            id = "dune-part-two",
            title = "Dune: Hành Tinh Cát 2",
            subtitle = "Dune: Part Two",
            year = "2024",
            ageRating = "T13",
            currentEpisode = "Full",
            genres = listOf("Viễn tưởng", "Phiêu lưu", "Chính kịch"),
            description = "Paul Atreides liên minh với người Fremen để báo thù những kẻ đã hủy diệt gia tộc anh. Trong khi bị giằng xé giữa tình yêu và định mệnh, Paul phải lựa chọn con đường có thể thay đổi toàn bộ tương lai thiên hà.",
            bannerUrl = "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg",
            posterUrl = "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg",
            episodes = listOf(
                EpisodeUi("dune-ep-1", "Bản chiếu rạp", "166 phút")
            ),
            cast = listOf(
                CastUi("Timothée Chalamet", "Paul Atreides"),
                CastUi("Zendaya", "Chani"),
                CastUi("Rebecca Ferguson", "Lady Jessica"),
                CastUi("Austin Butler", "Feyd-Rautha")
            ),
            recommendations = listOf(
                RecommendedMovieUi(
                    id = "interstellar",
                    title = "Interstellar",
                    year = "2014",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "oppenheimer",
                    title = "Oppenheimer",
                    year = "2023",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BN2JkMDc5MGQtZjg3YS00NmFiLWIyZmQtZjZjMjc3MmJlZTQ1XkEyXkFqcGc@._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "joker",
                    title = "Joker",
                    year = "2019",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00YTJmLWE0ZDctN2ZiYTk2YmI3NTYyXkEyXkFqcGdeQXVyMTkxNjUyNQ@@._V1_.jpg"
                )
            )
        ),
        MovieDetailUi(
            id = "interstellar",
            title = "Interstellar",
            subtitle = "Interstellar",
            year = "2014",
            ageRating = "T13",
            currentEpisode = "Full",
            genres = listOf("Khoa học", "Phiêu lưu", "Gia đình"),
            description = "Khi Trái Đất dần trở nên không còn phù hợp cho sự sống, một nhóm phi hành gia được giao nhiệm vụ băng qua lỗ sâu để tìm hành tinh mới cho loài người. Cuộc hành trình đặt ra những quyết định sinh tử về thời gian, tình yêu và sự hy sinh.",
            bannerUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
            posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_.jpg",
            episodes = listOf(
                EpisodeUi("interstellar-ep-1", "Bản chiếu rạp", "169 phút")
            ),
            cast = listOf(
                CastUi("Matthew McConaughey", "Cooper"),
                CastUi("Anne Hathaway", "Brand"),
                CastUi("Jessica Chastain", "Murph"),
                CastUi("Michael Caine", "Professor Brand")
            ),
            recommendations = listOf(
                RecommendedMovieUi(
                    id = "dune-part-two",
                    title = "Dune: Part Two",
                    year = "2024",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BN2QyZGU4ZDctOWMzMy00NTc5LThlOGQtODhmNDI1NmY5YzAwXkEyXkFqcGdeQXVyMDM2NDM2MQ@@._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "silo",
                    title = "Silo",
                    year = "2023",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMDI2NjQ1NjctZGI2MC00YzgyLTk3Y2ItMjkwMjNlODg3NTcwXkEyXkFqcGc@._V1_.jpg"
                ),
                RecommendedMovieUi(
                    id = "dark",
                    title = "Dark",
                    year = "2017",
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BOTk2NzUyOTctZDdlMS00MDJlLTgzNTEtNzQzYjFhNzY0ZGFmXkEyXkFqcGdeQXVyMjg1NDcxNDE@._V1_.jpg"
                )
            )
        )
    )

    private val movieById: Map<String, MovieDetailUi> = movies.associateBy { it.id }

    val searchMovies: List<SearchMovieUi> = listOf(
        SearchMovieUi("alice-in-borderland", "Thế Giới Không Lối", "Alice in Borderland", "Lồng Tiếng", "green", "7.8", "2020"),
        SearchMovieUi("dune-part-two", "Dune: Hành Tinh Cát 2", "Dune: Part Two", "Phụ Đề", "gray", "8.6", "2024"),
        SearchMovieUi("interstellar", "Interstellar", "Interstellar", "Phụ Đề", "gray", "8.7", "2014"),
        SearchMovieUi("joker", "Joker", "Joker", "Thuyết Minh", "blue", "8.4", "2019"),
        SearchMovieUi("squid-game", "Squid Game", "Squid Game", "Lồng Tiếng", "green", "8.0", "2021")
    )

    fun findMovie(movieId: String): MovieDetailUi? = movieById[movieId]

    fun findDefaultMovie(): MovieDetailUi = movies.first()
}
